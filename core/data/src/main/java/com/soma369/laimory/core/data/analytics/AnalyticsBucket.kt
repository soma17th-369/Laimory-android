package com.soma369.laimory.core.data.analytics

/**
 * 제품 분석 이벤트가 쌓이는 곳.
 *
 * 구현이 Firebase(GA4)·스프레드시트·자체 서버 중 무엇이든 이 모양만 지킨다 — 포트가 특정 SDK 의
 * 어휘를 드러내면 갈아 끼울 때 이벤트 정의와 호출부까지 함께 열어야 한다([CrashReporter] 와 같은
 * 원칙). 이벤트를 [AnalyticsPayload] 로 바꾸는 일은 버킷 밖에서 한 번만 하고, 버킷은 자기 대상의
 * 제약만 책임진다.
 *
 * 여러 버킷을 동시에 둘 수 있다. 옮길 때 두 곳으로 함께 보내 결과를 비교한 뒤 옛 버킷을 뺀다.
 */
interface AnalyticsBucket {
    /** 한 건을 보낸다. 실패는 예외로 알린다 — 삼키는 일은 호출부가 맡는다. */
    suspend fun send(payload: AnalyticsPayload)

    /**
     * 수집을 켜고 끈다.
     *
     * 동의를 받는 구조가 되면 그 상태를 이 스위치로 넘긴다. 끈 동안에는 아무것도 보내지 않고,
     * 나중에 다시 보내려고 쌓아 두지도 않는다.
     */
    fun setEnabled(enabled: Boolean)
}
