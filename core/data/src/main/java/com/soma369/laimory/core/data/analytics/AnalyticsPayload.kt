package com.soma369.laimory.core.data.analytics

/**
 * 버킷으로 보낼 한 건. 어느 전송 대상이든 받을 수 있는 공통 형태다.
 *
 * 값을 문자열과 정수로만 한정한다 — 스프레드시트 한 칸이든 GA4 파라미터든 이 둘은 그대로 들어간다.
 * 이름은 전송 대상에서 쓰이는 그대로이며, 가장 좁은 대상인 GA4 제한(이벤트 이름 40자 · 속성 25개 ·
 * 문자열 값 100자 · `firebase_`/`google_`/`ga_` 접두사 금지) 안에서 정한다. 그래야 버킷을 옮겨도
 * 잘리지 않는다.
 */
data class AnalyticsPayload(
    val name: String,
    val strings: Map<String, String> = emptyMap(),
    val counts: Map<String, Long> = emptyMap(),
)
