package com.soma369.laimory.analytics

import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKeys
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.InstallAttribution
import com.soma369.laimory.core.domain.model.analytics.InstallReferrerStatus
import com.soma369.laimory.core.domain.repository.InstallAttributionRepository
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 설치 유입 UTM 을 Play Install Referrer 로 읽어 설치 단위로 남기고, 분석의 사용자 속성으로 건다(#424).
 *
 * **기존 분석 흐름에 끼어들지 않는다.** 수집 시작 시점·다른 이벤트의 전송을 기다리게 하거나 바꾸지 않고
 * 백그라운드에서 따로 돈다. 그래서 SDK 가 먼저 기록하는 `first_open` 에는 속성이 붙지 않고, 그 뒤 이벤트와
 * `install_attribution_resolved` 로 캠페인을 본다. 실패는 상태 값만 경고로 남기고 무시한다.
 *
 * - 결과가 확정되면(파싱·공급자 값·캠페인 없음·거절·미지원) 다시 조회하지 않는다. Referrer 는 재설치 전까지
 *   바뀌지 않는다.
 * - 일시 오류는 실행마다 [MAX_ATTEMPTS] 번까지 다시 묻고, [MAX_FAILED_LAUNCHES] 번째 실행까지 이어지면
 *   [InstallReferrerStatus.UNAVAILABLE] 로 확정한다.
 * - 확정된 결과가 있으면 실행마다 사용자 속성을 다시 건다. 데이터 삭제로 SDK 상태만 사라진 경우를 덮고,
 *   같은 값을 다시 거는 것이라 비용이 없다.
 */
@Singleton
class InstallAttributionReporter
    @Inject
    internal constructor(
        private val source: InstallReferrerSource,
        private val repository: InstallAttributionRepository,
        private val analyticsHelper: AnalyticsHelper,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) {
        private var job: Job? = null

        fun start() {
            if (job?.isActive == true) return
            job = applicationScope.launch { report() }
        }

        internal suspend fun report() {
            try {
                val record = repository.load()
                val attribution = record.attribution ?: resolve() ?: return
                analyticsHelper.setInstallAttribution(attribution)
                // 수집이 꺼져 있어 못 보냈으면 판정 키가 남아(`logOnce` 계약) 다음 실행에서 다시 시도된다.
                analyticsHelper.logOnce(
                    AnalyticsDedupeKeys.installAttributionResolved(record.installId),
                    AnalyticsEvent.InstallAttributionResolved(attribution),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Logger.w(LogDomain.ANALYTICS, "설치 유입 확인 실패: cause=${error::class.simpleName}")
            }
        }

        /** 조회해 확정되면 남기고 돌려준다. 이번 실행에서 확정하지 못했으면 null. */
        private suspend fun resolve(): InstallAttribution? {
            val attribution =
                when (val lookup = lookupWithRetry()) {
                    is InstallReferrerLookup.Found ->
                        InstallReferrerParser.parse(lookup.referrer).copy(
                            clickAtMillis = lookup.clickAtSeconds.secondsToMillisOrNull(),
                            installBeginAtMillis = lookup.installBeginAtSeconds.secondsToMillisOrNull(),
                        )
                    InstallReferrerLookup.Unsupported -> InstallAttribution(InstallReferrerStatus.UNAVAILABLE)
                    InstallReferrerLookup.Transient -> {
                        val failedLaunches = repository.countFailedLaunch()
                        if (failedLaunches < MAX_FAILED_LAUNCHES) {
                            Logger.i(LogDomain.ANALYTICS, "설치 유입 조회 보류: failedLaunches=$failedLaunches")
                            return null
                        }
                        InstallAttribution(InstallReferrerStatus.UNAVAILABLE)
                    }
                }
            repository.saveResolved(attribution)
            // 원문·캠페인 값은 남기지 않는다. 상태만.
            Logger.i(LogDomain.ANALYTICS, "설치 유입 확정: status=${attribution.status}")
            return attribution
        }

        private suspend fun lookupWithRetry(): InstallReferrerLookup {
            var attempt = 1
            while (true) {
                val lookup = source.fetch()
                if (lookup !is InstallReferrerLookup.Transient || attempt >= MAX_ATTEMPTS) return lookup
                delay(RETRY_DELAY_MILLIS * attempt)
                attempt++
            }
        }

        private fun Long.secondsToMillisOrNull(): Long? = takeIf { it > 0 }?.times(MILLIS_PER_SECOND)

        internal companion object {
            const val MAX_ATTEMPTS = 3
            const val MAX_FAILED_LAUNCHES = 5
            const val RETRY_DELAY_MILLIS = 2_000L
            private const val MILLIS_PER_SECOND = 1_000L
        }
    }
