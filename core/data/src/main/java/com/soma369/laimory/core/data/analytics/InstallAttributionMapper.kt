package com.soma369.laimory.core.data.analytics

import com.soma369.laimory.core.domain.model.analytics.InstallAttribution
import com.soma369.laimory.core.domain.model.analytics.InstallReferrerStatus

/*
 * 설치 유입을 전송 형태로 바꾼다. 이벤트 매퍼와 같은 원칙으로 이름과 값을 여기 적힌 문자열로 고정한다 —
 * 대시보드·BigQuery 의 사용자 범위 측정기준이 이 이름에 묶인다.
 */

/** 앱 경로의 귀속 방식. Google Ads 직접 연동 귀속은 GA4 쪽 결과라 앱이 만들지 않는다. */
private const val ATTRIBUTION_METHOD_PLAY_INSTALL_REFERRER = "play_install_referrer"

private const val PARAM_ATTRIBUTION_METHOD = "attribution_method"
private const val PARAM_REFERRER_STATUS = "referrer_status"
private const val PARAM_UTM_SOURCE = "utm_source"
private const val PARAM_UTM_MEDIUM = "utm_medium"
private const val PARAM_UTM_CAMPAIGN = "utm_campaign"
private const val PARAM_UTM_CONTENT = "utm_content"
private const val PARAM_UTM_ID = "utm_id"

internal const val USER_PROPERTY_REFERRER_STATUS = "referrer_status"
internal const val USER_PROPERTY_INSTALL_SOURCE = "install_source"
internal const val USER_PROPERTY_INSTALL_MEDIUM = "install_medium"
internal const val USER_PROPERTY_INSTALL_CAMPAIGN = "install_campaign"
internal const val USER_PROPERTY_INSTALL_CONTENT = "install_content"
internal const val USER_PROPERTY_INSTALL_CAMPAIGN_ID = "install_campaign_id"

/** GA4 사용자 속성 값 한도. 넘는 값은 자르지 않고 걸지 않는다 — 잘린 캠페인 값은 다른 캠페인이 된다. */
internal const val USER_PROPERTY_VALUE_MAX_LENGTH = 36

/** `install_attribution_resolved` 의 문자열 속성. 캠페인 값은 있는 것만 싣는다(이벤트 값 한도 100자 안). */
internal fun InstallAttribution.toEventStrings(): Map<String, String> =
    buildMap {
        put(PARAM_ATTRIBUTION_METHOD, ATTRIBUTION_METHOD_PLAY_INSTALL_REFERRER)
        put(PARAM_REFERRER_STATUS, status.paramValue)
        campaign?.let { campaign ->
            put(PARAM_UTM_SOURCE, campaign.source)
            campaign.medium?.let { put(PARAM_UTM_MEDIUM, it) }
            campaign.campaign?.let { put(PARAM_UTM_CAMPAIGN, it) }
            campaign.content?.let { put(PARAM_UTM_CONTENT, it) }
            campaign.campaignId?.let { put(PARAM_UTM_ID, it) }
        }
    }

/**
 * 버킷에 걸 사용자 속성. 값이 null 인 속성은 **푼다** — 재설치 뒤 SDK 에 옛 값이 남아 있더라도 이 설치의
 * 결과와 어긋나지 않게 모든 이름을 매번 정한다.
 *
 * 캠페인을 싣지 않는 상태면 `referrer_status` 만 건다. 36자를 넘는 값은 걸지 않고 이벤트로만 남긴다.
 */
internal fun InstallAttribution.toUserProperties(): Map<String, String?> {
    val campaign = campaign
    return mapOf(
        USER_PROPERTY_REFERRER_STATUS to status.paramValue,
        USER_PROPERTY_INSTALL_SOURCE to campaign?.source.fitUserProperty(),
        USER_PROPERTY_INSTALL_MEDIUM to campaign?.medium.fitUserProperty(),
        USER_PROPERTY_INSTALL_CAMPAIGN to campaign?.campaign.fitUserProperty(),
        USER_PROPERTY_INSTALL_CONTENT to campaign?.content.fitUserProperty(),
        USER_PROPERTY_INSTALL_CAMPAIGN_ID to campaign?.campaignId.fitUserProperty(),
    )
}

private fun String?.fitUserProperty(): String? = this?.takeIf { value -> value.length <= USER_PROPERTY_VALUE_MAX_LENGTH }

private val InstallReferrerStatus.paramValue: String
    get() =
        when (this) {
            InstallReferrerStatus.PARSED -> "parsed"
            InstallReferrerStatus.PROVIDER -> "provider"
            InstallReferrerStatus.NO_CAMPAIGN -> "no_campaign"
            InstallReferrerStatus.INVALID -> "invalid"
            InstallReferrerStatus.UNAVAILABLE -> "unavailable"
        }
