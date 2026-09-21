package com.soma369.laimory.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.soma369.laimory.core.data.analytics.AnalyticsBucket
import com.soma369.laimory.core.data.analytics.AnalyticsPayload

/**
 * GA4 로 보내는 버킷. Firebase 를 아는 것은 이 클래스뿐이다.
 *
 * `logEvent` 는 호출한 쪽을 기다리게 하지 않는다 — 묶어 보내기·재시도·오프라인 대기는 SDK 가 맡아
 * 우리가 큐를 둘 이유가 없다.
 */
internal class FirebaseAnalyticsBucket(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsBucket {
    override suspend fun send(payload: AnalyticsPayload) {
        val parameters =
            Bundle().apply {
                payload.strings.forEach { (key, value) -> putString(key, value) }
                payload.counts.forEach { (key, value) -> putLong(key, value) }
            }
        firebaseAnalytics.logEvent(payload.name, parameters)
    }

    override fun setEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }
}
