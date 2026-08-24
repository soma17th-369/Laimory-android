package com.soma369.laimory.core.domain.provider

/**
 * 자동 수집을 시도해도 되는 플랫폼 상태를 알려 주는 포트.
 *
 * Android 권한과 Health Connect SDK 상태는 도메인이 알 수 없으므로 구현을 밖에 둔다.
 * 수집 Coordinator 가 `Context` 나 Health Connect 타입을 직접 참조하지 않게 하려는 경계다.
 *
 * 판정만 하고 **요청하지 않는다.** 자동 수집은 사용자가 시작한 동작이 아니라 권한 요청 화면을
 * 띄우면 안 된다 — 요청은 온보딩 화면의 몫이다.
 */
interface CollectionAvailabilityProvider {
    /** 캘린더 읽기 권한이 이미 허용돼 있는지. */
    fun canCollectCalendar(): Boolean

    /** Health Connect 를 쓸 수 있는지(설치·버전). 권한과 별개다. */
    fun isHealthConnectAvailable(): Boolean

    /** 걸음수·수면 읽기 권한이 이미 허용돼 있는지. Health Connect 는 비동기로만 확인할 수 있다. */
    suspend fun canCollectHealth(): Boolean
}
