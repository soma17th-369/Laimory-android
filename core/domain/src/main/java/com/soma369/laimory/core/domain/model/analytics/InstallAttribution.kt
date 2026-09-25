package com.soma369.laimory.core.domain.model.analytics

/**
 * 이 설치가 어떤 캠페인 링크를 거쳐 들어왔는지. Play Install Referrer 를 읽어 정제한 값이다.
 *
 * **계정이 아니라 설치에 딸린다** — 로그아웃·계정 전환이 지우지 않는다. 원문 referrer 는 담지 않는다.
 * UTM 은 캠페인 라벨일 뿐 사용자 신원이나 진짜 광고 클릭을 인증하지 않는다.
 *
 * [campaign] 은 [status] 가 캠페인 값을 싣는 상태([InstallReferrerStatus.carriesCampaign])일 때만 있다.
 * 시각은 Play 가 준 값(초)을 UTC 밀리초로 옮긴 것이고, Play 가 주지 않으면(0) 없다.
 */
data class InstallAttribution(
    val status: InstallReferrerStatus,
    val campaign: InstallCampaign? = null,
    val clickAtMillis: Long? = null,
    val installBeginAtMillis: Long? = null,
) {
    init {
        require((campaign != null) == status.carriesCampaign) { "campaign must match status" }
    }
}

/**
 * 검증을 통과한 UTM 값. 수동 캠페인(Meta)이면 [medium]·[campaign] 이 반드시 있고, 공급자 값(Google·Play)은
 * 받은 것만 있다.
 */
data class InstallCampaign(
    val source: String,
    val medium: String? = null,
    val campaign: String? = null,
    val content: String? = null,
    val campaignId: String? = null,
)

/** Referrer 조회·검증 결과. 앱 경로의 귀속 방식은 항상 Play Install Referrer 다. */
enum class InstallReferrerStatus(
    val carriesCampaign: Boolean,
) {
    /** 수동 캠페인 값(Meta)이 허용 목록·형식 검증을 통과했다. */
    PARSED(carriesCampaign = true),

    /** Google·Play 가 붙인 공급자 값. 규칙으로 거르지 않고 원값을 보존한다. organic 판정은 분석 계층이 한다. */
    PROVIDER(carriesCampaign = true),

    /** 조회는 됐지만 UTM 이 없다. organic 으로 단정하지 않는다. */
    NO_CAMPAIGN(carriesCampaign = false),

    /** 중복 키·잘못된 인코딩·허용 밖 값·필수 키 누락·형식 위반. */
    INVALID(carriesCampaign = false),

    /** 기능 미지원이거나 일시 오류가 제한된 재시도 뒤에도 이어졌다. */
    UNAVAILABLE(carriesCampaign = false),
}

/**
 * 기기에 남긴 설치 귀속 상태.
 *
 * [installId] 는 이 설치를 가르는 무작위 값이다. 기기 안에만 있고 전송되지 않는다 — 한 번만 보내는 이벤트의
 * 판정 키를 설치마다 새로 만들어, 백업에서 복원된 옛 판정 키에 새 설치의 이벤트가 막히지 않게 한다.
 * [attribution] 이 있으면 결과가 확정된 것이라 다시 조회하지 않는다.
 */
data class InstallAttributionRecord(
    val installId: String,
    val attribution: InstallAttribution?,
    val failedLaunchCount: Int,
)
