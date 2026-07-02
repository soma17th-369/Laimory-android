package com.soma369.laimory.core.domain.notification

/**
 * 도메인이 공통 정책성 알림을 발행하는 의미 수준 포트.
 *
 * presentation 구현체가 [UserNotification]을 다이얼로그/스낵바/네비게이션 등 실제 UI 효과로 매핑한다.
 */
interface UserNotifier {
    fun notify(notification: UserNotification)
}
