package com.soma369.laimory.core.domain.notification

/**
 * 공통 정책성 사용자 알림의 의미 수준 표현.
 *
 * 스낵바/다이얼로그 같은 UI 표현을 직접 말하지 않는다. presentation 구현체가 실제 UI 효과로 매핑한다.
 */
sealed interface UserNotification {
    /** 세션 만료 — 재로그인 유도. */
    data object SessionExpired : UserNotification

    /** 일시적 오류 — 잠시 후 재시도. */
    data object TemporaryUnavailable : UserNotification
}
