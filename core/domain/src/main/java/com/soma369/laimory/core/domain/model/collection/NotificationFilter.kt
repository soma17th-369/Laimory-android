package com.soma369.laimory.core.domain.model.collection

/**
 * 알림 수집 필터 설정.
 *
 * [mode] 가 [NotificationCollectMode.ALL] 이면 모든 알림을 수집하고, [NotificationCollectMode.SELECTIVE] 이면
 * `키워드([keywords]) OR 앱([allowedPackages]) OR 클릭` 중 하나라도 맞는 알림만 수집한다.
 * 클릭 수집은 사용자가 알림창에서 탭한 알림으로, 필터 값과 무관하게 선택 모드에서 항상 대상이다.
 */
data class NotificationFilter(
    val mode: NotificationCollectMode = NotificationCollectMode.SELECTIVE,
    /** 제목/본문에 이 중 하나라도 포함되면 수집(대소문자 무시). */
    val keywords: Set<String> = emptySet(),
    /** 이 패키지들의 알림을 수집(allowlist). */
    val allowedPackages: Set<String> = emptySet(),
)

enum class NotificationCollectMode {
    /** 모든 알림을 수집한다. */
    ALL,

    /** 키워드 OR 앱 OR 클릭에 해당하는 알림만 수집한다. */
    SELECTIVE,
}
