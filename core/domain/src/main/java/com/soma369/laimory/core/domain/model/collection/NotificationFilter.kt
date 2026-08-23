package com.soma369.laimory.core.domain.model.collection

/**
 * 알림 수집 필터 설정.
 *
 * `클릭 OR 키워드 OR 앱([allowedPackages])` 중 하나라도 맞는 알림만 수집한다.
 * 클릭 수집이 [collectOnClick]로 켜져 있으면(기본값), 사용자가 알림창에서 탭한 알림은
 * 키워드·앱 설정과 무관하게 수집한다.
 *
 * 기본 키워드는 [DEFAULT_KEYWORD_SCOPES]가 정한 도메인별 앱에서만 걸린다. 사용자가 직접
 * 등록한 [keywords]에는 이 범위를 적용하지 않는다.
 *
 * 개인정보 정책([NotificationPrivacyPolicy])을 통과한 텍스트로만 판정한다 —
 * 보호 대상 정보는 이 필터의 어느 경로로도 수집되지 않는다.
 */
data class NotificationFilter(
    /** 사용자가 알림창에서 클릭한 알림을 수집할지 여부. 기존 설정 호환을 위해 기본값은 true다. */
    val collectOnClick: Boolean = true,
    /** 앱이 내장한 기본 키워드([DEFAULT_KEYWORD_SCOPES])를 함께 쓸지 여부. */
    val useDefaultKeywords: Boolean = true,
    /** 사용자가 직접 등록한 키워드. 기본 키워드는 여기에 병합 저장하지 않는다. */
    val keywords: Set<String> = emptySet(),
    /** 이 패키지들의 알림을 수집(allowlist). */
    val allowedPackages: Set<String> = emptySet(),
) {
    /**
     * 알림의 수집 사유를 결정한다.
     *
     * 판정 순서는 `클릭 → 비이벤트 제외 → 키워드 → 앱`이다. 클릭은 명시적 사용자 행동이므로
     * 비이벤트 제외를 적용하지 않고, 앱 allowlist 와 키워드 경로에만 적용한다.
     * 빈 키워드는 설정에 남아 있더라도 일치 조건에서 제외한다.
     *
     * 광고 표기([hasAdvertisementMarker])와 읽을 수 없는 본문([NotificationSignals.hasUnreadableBody])은
     * 키워드 경로에만 적용한다 — 키워드는 사용자가 앱을 고르지 않아도 걸리는 넓은 경로라 광고가
     * 섞이지만, 클릭과 앱 allowlist 는 사용자가 그 알림·그 앱을 직접 지목한 결과다. 그래서 둘 중
     * 하나에 해당하는 allowlist 앱의 알림은 키워드가 걸리더라도
     * [NotificationPayload.CollectReason.APP]으로 수집된다.
     *
     * 본문을 읽을 수 없으면 키워드 경로로 수집하지 않는다 — 광고 표기가 그 본문에 있으면 판정
     * 자체가 성립하지 않으므로, 제목 한 줄만 보고 긁어오는 것은 근거가 없는 수집이다. 저장되는
     * 값도 제목뿐이라 초안 품질에도 기여하지 않는다.
     *
     * 키워드는 사용자 입력과 기본 사전을 **합치기 전에** 나눠 판정한다. 합친 뒤 앱 범위를
     * 걸면 사용자가 직접 등록한 `도착` 까지 목록 밖 앱에서 막힌다.
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
        val matchesUserKeyword = keywords.containsKeywordIn(content)
        val matchesScopedDefault =
            useDefaultKeywords && DEFAULT_KEYWORD_SCOPES.any { it.matches(packageName, content) }

        return when {
            (matchesUserKeyword || matchesScopedDefault) &&
                !signals.hasUnreadableBody &&
                !content.hasAdvertisementMarker() ->
                NotificationPayload.CollectReason.KEYWORD

            packageName in allowedPackages -> NotificationPayload.CollectReason.APP
            else -> null
        }
    }

    companion object {
        /**
         * 앱이 내장하는 기본 키워드 전체. [DEFAULT_KEYWORD_SCOPES] 의 합집합이다.
         *
         * 표시용이며 판정에는 쓰지 않는다 — 판정은 도메인별 앱 범위와 함께 봐야 한다.
         * 사전에서 파생시켜 어느 scope 에도 속하지 않은 키워드가 목록에만 남는 일을 막는다.
         *
         * 생활 이벤트의 상태 변화를 나타내는 말만 담는다. 부분 일치로 판정하므로 광고 문구에
         * 걸리는 경우가 있으며(`신상품 도착`), 구조 신호로도 광고 표기로도 잡히지 않는 광고는
         * 알려진 한계로 둔다.
         */
        val DEFAULT_KEYWORDS: Set<String> =
            DEFAULT_KEYWORD_SCOPES.flatMapTo(linkedSetOf()) { it.keywords }
    }
}

/**
 * 정보통신망법이 광고성 정보에 강제하는 제목 표기가 있는지 본다.
 *
 * `무료`·`특가` 같은 광고 문구 사전은 정상 이벤트와 같은 단어를 써서 오탐이 크지만, 괄호로
 * 감싼 광고 표기는 법이 정한 형식이라 배송·결제 알림이 쓸 수 없다. 텍스트로 드러난 구조
 * 신호로 보고, 앱이 `CATEGORY_PROMO`를 선언하지 않는 광고를 [NotificationSignals] 대신 잡는다.
 *
 * 괄호 **안**의 광고 표기만 본다 — `[CJ대한통운] 배송완료`, `[Web발신]` 같은 정상 접두와
 * [NotificationPrivacyPolicy]가 넣는 `[전화번호]`·`[상세주소]` 마스킹 토큰을 함께 날리지
 * 않기 위해서다. 수집 판정은 마스킹을 거친 텍스트로 하므로 후자가 특히 중요하다.
 */
private fun String.hasAdvertisementMarker(): Boolean = ADVERTISEMENT_MARKER.containsMatchIn(this)

/**
 * 괄호로 감싼 광고 표기. 허용 표기를 명시해 `(광고)`, `[광고]`, `(동영상 광고)`, `(광고성 정보)`만 잡는다.
 *
 * 괄호 안을 임의의 앞뒤 몇 글자로 열어 두면 `(비광고)`, `(광고 없음)`, `(광고 차단)` 같은 부정
 * 표현까지 걸린다. 그러면 정상 알림이 키워드 경로에서 통째로 빠지므로, 표기를 못 잡아 광고가
 * 섞이는 쪽보다 손해가 크다. 새 표기가 확인되면 여기에 더한다.
 */
private val ADVERTISEMENT_MARKER =
    Regex("""[(\[【（]\s*(?:광고|동영상\s*광고|광고성\s*정보)\s*[)\]】）]""")
