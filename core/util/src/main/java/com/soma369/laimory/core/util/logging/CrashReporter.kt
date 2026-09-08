package com.soma369.laimory.core.util.logging

/**
 * 크래시 리포트에 맥락을 실어 보내는 원격 대상.
 *
 * 구현은 app 모듈이 가진다. 이 모듈은 어떤 SDK 가 뒤에 있는지 알지 않는다 — 수집 대상은 바뀔 수
 * 있는 선택이고(Crashlytics · Sentry · 자체 수집), 포트가 특정 SDK 의 타입이나 어휘를 드러내면
 * 갈아 끼울 때 이 모듈부터 열어야 한다. 그 시점에는 이미 앱 전체가 그 어휘에 묶여 있다.
 *
 * 그래서 **어느 구현으로도 옮길 수 있는 최소한의 동사**만 둔다. 값은 문자열 하나로 통일한다 —
 * 타입별 오버로드는 SDK 마다 지원 범위가 달라 포트를 특정 구현 쪽으로 기울인다.
 *
 * 민감정보: 여기로 넘기는 문장은 **기기를 떠나 외부로 나간다.** [Logger] 의 "객체 전체 dump 금지"
 * 정책이 그대로, 더 엄하게 적용된다. 기록 본문·사진 경로·좌표·주소·토큰·이메일·닉네임을 넣지
 * 않는다. 식별자와 상태 요약만 남긴다.
 */
interface CrashReporter {
    /** 크래시 직전 맥락으로 남길 한 줄(브레드크럼). */
    fun log(message: String)

    /** 앱을 죽이지 않았지만 알아야 할 예외(non-fatal). */
    fun recordException(throwable: Throwable)

    /** 리포트를 가를 때 쓰는 꼬리표. 같은 키를 다시 넣으면 덮어쓴다. */
    fun setKey(
        key: String,
        value: String,
    )
}
