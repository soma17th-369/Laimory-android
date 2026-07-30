package com.soma369.laimory.core.domain.model.collection

/**
 * 알림 수집 필터 설정.
 *
 * `클릭 OR 키워드([keywords]) OR 앱([allowedPackages])` 중 하나라도 맞는 알림만 수집한다.
 * 클릭 수집은 사용자가 알림창에서 탭한 알림으로, 키워드·앱 설정과 무관하게 항상 대상이다.
 */
data class NotificationFilter(
    /** 사용자가 알림창에서 클릭한 알림을 수집할지 여부. 기존 설정 호환을 위해 기본값은 true다. */
    val collectOnClick: Boolean = true,
    /** 제목/본문에 이 중 하나라도 포함되면 수집(대소문자 무시). */
    val keywords: Set<String> = emptySet(),
    /** 이 패키지들의 알림을 수집(allowlist). */
    val allowedPackages: Set<String> = emptySet(),
) {
    /**
     * 알림의 수집 사유를 결정한다.
     *
     * 여러 조건이 동시에 맞으면 사용자 행동인 클릭을 먼저, 그다음 키워드, 앱 순으로 대표 사유를 선택한다.
     * 빈 키워드는 설정에 남아 있더라도 일치 조건에서 제외한다.
     */
    fun collectReasonFor(
        packageName: String,
        title: String?,
        text: String?,
        clicked: Boolean,
    ): NotificationPayload.CollectReason? {
        if (clicked) {
            return NotificationPayload.CollectReason.CLICK.takeIf { collectOnClick }
        }

        val content = listOfNotNull(title, text).joinToString(" ")
        return when {
            keywords.any { it.isNotBlank() && content.contains(it, ignoreCase = true) } ->
                NotificationPayload.CollectReason.KEYWORD

            packageName in allowedPackages -> NotificationPayload.CollectReason.APP
            else -> null
        }
    }
}
