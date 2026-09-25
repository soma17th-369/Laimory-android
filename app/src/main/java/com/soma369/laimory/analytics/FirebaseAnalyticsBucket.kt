package com.soma369.laimory.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.soma369.laimory.core.data.analytics.AnalyticsBucket
import com.soma369.laimory.core.data.analytics.AnalyticsPayload
import java.util.concurrent.atomic.AtomicBoolean

/**
 * GA4 로 보내는 버킷. Firebase 를 아는 것은 이 클래스뿐이다.
 *
 * `logEvent` 는 호출한 쪽을 기다리게 하지 않는다 — 묶어 보내기·재시도·오프라인 대기는 SDK 가 맡아
 * 우리가 큐를 둘 이유가 없다.
 *
 * @param initiallyEnabled 매니페스트가 정한 시작 상태. SDK 는 현재 수집 여부를 되돌려 주지 않으므로
 *   우리가 시작값과 이후 [setEnabled] 를 기억한다. 값을 코드에 또 적지 않고 매니페스트에서 읽어
 *   두 곳이 어긋나지 않게 한다.
 */
internal class FirebaseAnalyticsBucket(
    private val firebaseAnalytics: FirebaseAnalytics,
    initiallyEnabled: Boolean,
) : AnalyticsBucket {
    private val enabled = AtomicBoolean(initiallyEnabled)

    override val isEnabled: Boolean get() = enabled.get()

    override suspend fun send(payload: AnalyticsPayload) {
        val parameters =
            Bundle().apply {
                payload.strings.forEach { (key, value) -> putString(key, value) }
                payload.counts.forEach { (key, value) -> putLong(key, value) }
            }
        firebaseAnalytics.logEvent(payload.name, parameters)
    }

    override fun setUserProperty(
        name: String,
        value: String?,
    ) {
        firebaseAnalytics.setUserProperty(name, value)
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled.set(enabled)
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }

    /** GA4 User-ID. SDK 가 앱 재시작 뒤에도 기억하므로 로그아웃 때 null 로 풀어야 한다. */
    override fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
    }
}
