package com.soma369.laimory.core.data.helper

import com.soma369.laimory.core.data.analytics.AnalyticsBucket
import com.soma369.laimory.core.data.analytics.AnalyticsDedupeStore
import com.soma369.laimory.core.data.analytics.toPayload
import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKey
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 이벤트를 전송 형태로 바꿔 모든 버킷에 보낸다.
 *
 * **분석은 앱 동작을 막거나 죽이지 않는다.** 버킷 하나가 실패해도 남은 버킷은 그대로 보내고,
 * 실패는 경고 로그로만 남긴다(원문이 새지 않게 예외 종류만 적는다).
 */
@Singleton
internal class AnalyticsHelperImpl
    @Inject
    constructor(
        private val buckets: Set<@JvmSuppressWildcards AnalyticsBucket>,
        private val dedupeStore: AnalyticsDedupeStore,
    ) : AnalyticsHelper {
        override suspend fun log(event: AnalyticsEvent) {
            dispatch(event)
        }

        override suspend fun logOnce(
            key: AnalyticsDedupeKey,
            event: AnalyticsEvent,
        ) {
            val first =
                try {
                    dedupeStore.markIfFirst(key.value)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    // 판정 기록을 못 읽었다고 이벤트를 두 번 보내지는 않는다 — 한 번만 나가야 하는
                    // 이벤트라 중복보다 누락이 낫다. 재시도는 다음 관찰이 알아서 한다.
                    Logger.w(LogDomain.ANALYTICS, "중복 방지 판정 실패: ${error::class.simpleName}")
                    return
                }
            if (!first) return
            dispatch(event)
        }

        private suspend fun dispatch(event: AnalyticsEvent) {
            val payload = event.toPayload()
            buckets.forEach { bucket ->
                try {
                    bucket.send(payload)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Logger.w(
                        LogDomain.ANALYTICS,
                        "분석 이벤트 전송 실패: event=${payload.name}, cause=${error::class.simpleName}",
                    )
                }
            }
        }
    }
