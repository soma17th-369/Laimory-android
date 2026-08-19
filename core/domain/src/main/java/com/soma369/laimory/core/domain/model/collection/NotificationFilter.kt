package com.soma369.laimory.core.domain.model.collection

/**
 * 알림 수집 필터 설정.
 *
 * `클릭 OR 키워드 OR 앱([allowedPackages])` 중 하나라도 맞는 알림만 수집한다.
 * 클릭 수집이 [collectOnClick]로 켜져 있으면(기본값), 사용자가 알림창에서 탭한 알림은
 * 키워드·앱 설정과 무관하게 수집한다.
 *
 * 개인정보 정책([NotificationPrivacyPolicy])을 통과한 텍스트로만 판정한다 —
 * 보호 대상 정보는 이 필터의 어느 경로로도 수집되지 않는다.
 */
data class NotificationFilter(
    /** 사용자가 알림창에서 클릭한 알림을 수집할지 여부. 기존 설정 호환을 위해 기본값은 true다. */
    val collectOnClick: Boolean = true,
    /** 앱이 내장한 기본 키워드([DEFAULT_KEYWORDS])를 함께 쓸지 여부. */
    val useDefaultKeywords: Boolean = true,
    /** 사용자가 직접 등록한 키워드. 기본 키워드는 여기에 병합 저장하지 않는다. */
    val keywords: Set<String> = emptySet(),
    /** 이 패키지들의 알림을 수집(allowlist). */
    val allowedPackages: Set<String> = emptySet(),
) {
    /**
     * 판정에 실제로 쓰는 키워드.
     *
     * DataStore 에는 사용자 입력만 남기고 기본 사전은 런타임에 합친다 — 병합 저장하면 사용자가
     * 지운 기본 키워드가 되살아나고, 앱이 사전을 고쳐도 기존 사용자에게 반영되지 않는다.
     */
    private val effectiveKeywords: Set<String> =
        if (useDefaultKeywords) keywords + DEFAULT_KEYWORDS else keywords

    /**
     * 알림의 수집 사유를 결정한다.
     *
     * 판정 순서는 `클릭 → 비이벤트 제외 → 키워드 → 앱`이다. 클릭은 명시적 사용자 행동이므로
     * 비이벤트 제외를 적용하지 않고, 앱 allowlist 와 키워드 경로에만 적용한다.
     * 빈 키워드는 설정에 남아 있더라도 일치 조건에서 제외한다.
     *
     * @param signals 리스너 경계에서 얻은 구조 신호. 알 수 없는 경계에서는 기본값을 쓰며
     *   이때 비이벤트 제외는 적용되지 않는다.
     */
    fun collectReasonFor(
        packageName: String,
        title: String?,
        text: String?,
        clicked: Boolean,
        signals: NotificationSignals = NotificationSignals.UNAVAILABLE,
    ): NotificationPayload.CollectReason? {
        if (clicked) {
            return NotificationPayload.CollectReason.CLICK.takeIf { collectOnClick }
        }
        if (signals.isNonEvent) return null

        val content = listOfNotNull(title, text).joinToString(" ")
        return when {
            effectiveKeywords.any { it.isNotBlank() && content.contains(it, ignoreCase = true) } ->
                NotificationPayload.CollectReason.KEYWORD

            packageName in allowedPackages -> NotificationPayload.CollectReason.APP
            else -> null
        }
    }

    companion object {
        /**
         * 앱이 내장하는 1차 기본 키워드.
         *
         * 생활 이벤트의 상태 변화를 나타내는 말만 담는다. 부분 일치로 판정하므로 광고 문구에
         * 걸리는 경우가 있으며(`신상품 도착`), 구조 신호로 잡히지 않는 광고는 알려진 한계로 둔다.
         */
        val DEFAULT_KEYWORDS: Set<String> =
            setOf(
                "결제", "승인", "환불",
                "주문", "배송", "배달", "픽업",
                "예약", "예매",
                "출발", "도착", "탑승",
                "취소",
            )
    }
}
