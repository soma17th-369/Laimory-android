package com.soma369.laimory.core.data.analytics

import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.analytics.AnalyticsConsent
import com.soma369.laimory.core.domain.repository.AnalyticsConsentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 저장된 수집 동의를 버킷에 적용한다. 앱이 뜰 때 한 번, 이후 값이 바뀔 때마다.
 *
 * | 동의 | 적용 |
 * | --- | --- |
 * | 미결정 | 손대지 않는다 — 빌드 기본값(매니페스트)을 따른다. release·qa 는 꺼짐, debug 는 켜짐 |
 * | 동의 | 켠다 |
 * | 거부 | 끈다(debug 포함) |
 *
 * 미결정일 때 끄지 않는 이유는 debug 에서 DebugView 로 확인할 수 있어야 해서다. release 는 매니페스트가
 * 이미 꺼 두므로 결과가 같다.
 *
 * **다른 초기화보다 먼저 시작한다.** 적용 전에 나온 이벤트는 버킷이 꺼져 있어 버려질 수 있지만,
 * 한 번만 보내는 이벤트는 판정 키를 소비하지 않으므로 다음 관찰에서 다시 시도된다.
 */
@Singleton
class AnalyticsConsentApplier
    @Inject
    constructor(
        private val buckets: Set<@JvmSuppressWildcards AnalyticsBucket>,
        private val consentRepository: AnalyticsConsentRepository,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) {
        private var job: Job? = null

        fun start() {
            if (job?.isActive == true) return
            job =
                applicationScope.launch {
                    consentRepository.consent.distinctUntilChanged().collect(::apply)
                }
        }

        internal fun apply(consent: AnalyticsConsent) {
            val enabled =
                when (consent) {
                    AnalyticsConsent.UNDECIDED -> return
                    AnalyticsConsent.GRANTED -> true
                    AnalyticsConsent.DENIED -> false
                }
            buckets.forEach { bucket -> bucket.setEnabled(enabled) }
        }
    }
