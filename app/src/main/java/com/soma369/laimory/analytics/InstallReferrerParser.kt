package com.soma369.laimory.analytics

import com.soma369.laimory.core.domain.model.analytics.InstallAttribution
import com.soma369.laimory.core.domain.model.analytics.InstallCampaign
import com.soma369.laimory.core.domain.model.analytics.InstallReferrerStatus
import java.net.URLDecoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Play 가 준 referrer 문자열을 검증해 정제값으로 바꾼다. 규칙은 마케팅 04 문서 §2.2 · §2.4 를 따른다.
 *
 * - **한 번만 해석한다.** API 가 준 문자열을 query 로 나누고 각 조각을 한 번 decode 한다. 결과를 다시
 *   decode 하지 않는다 — 두 번 풀면 값 안의 `%` 가 다른 글자로 바뀐다.
 * - **수동 캠페인(Meta)** 은 source·medium 을 허용 목록으로, campaign·content·id 를 형식으로만 본다.
 *   등록 여부는 앱이 모른다(실험 레지스트리 대조는 분석 계층).
 * - **공급자 값(Google·Play)** 은 거르거나 고치지 않고 형식만 본다. 대문자처럼 형식에 맞지 않으면 바꾸지
 *   않고 [InstallReferrerStatus.INVALID] 로 둔다.
 * - 허용 키 밖(`gclid`·`utm_term` 등)은 버린다.
 * - Play 가 값을 모를 때 채우는 `(not set)` 은 **키가 없는 것**으로 본다. 우리 링크가 깨진 것이 아니라서
 *   [InstallReferrerStatus.INVALID] 로 두면 링크 오류 신호에 일반 설치가 섞이고, 결과는 한 번 확정되면 다시
 *   조회하지 않아 그 설치에 영영 남는다.
 *
 * 원문은 이 함수 안에서만 다룬다. 저장·로그·전송하지 않는다.
 */
internal object InstallReferrerParser {
    fun parse(referrer: String?): InstallAttribution {
        if (referrer.isNullOrBlank()) return NO_CAMPAIGN
        val seenKeys = mutableSetOf<String>()
        val values = mutableMapOf<String, String>()
        for (segment in referrer.split('&')) {
            if (segment.isEmpty()) continue
            val separator = segment.indexOf('=')
            val rawKey = if (separator < 0) segment else segment.substring(0, separator)
            val key = rawKey.decodeOrNull() ?: return INVALID
            if (key !in ALLOWED_KEYS) continue
            val value = (if (separator < 0) "" else segment.substring(separator + 1)).decodeOrNull() ?: return INVALID
            // 값이 `(not set)` 이어도 같은 키가 두 번 온 것은 거절한다.
            if (!seenKeys.add(key)) return INVALID
            if (value == NOT_SET) continue
            values[key] = value
        }
        if (values.isEmpty()) return NO_CAMPAIGN
        val source = values[KEY_SOURCE] ?: return INVALID
        val campaign =
            if (source in MANUAL_SOURCES) {
                manualCampaign(source, values)
            } else {
                providerCampaign(source, values)
            } ?: return INVALID
        val status = if (source in MANUAL_SOURCES) InstallReferrerStatus.PARSED else InstallReferrerStatus.PROVIDER
        return InstallAttribution(status = status, campaign = campaign)
    }

    private fun manualCampaign(
        source: String,
        values: Map<String, String>,
    ): InstallCampaign? {
        val medium = values[KEY_MEDIUM]?.takeIf { it in MANUAL_MEDIUMS } ?: return null
        val campaign = values[KEY_CAMPAIGN]?.takeIf(::isManualCampaign) ?: return null
        val content = values[KEY_CONTENT]
        if (content != null && !isId(content)) return null
        val campaignId = values[KEY_ID]
        if (campaignId != null && !isCampaignId(campaignId)) return null
        return InstallCampaign(source = source, medium = medium, campaign = campaign, content = content, campaignId = campaignId)
    }

    private fun providerCampaign(
        source: String,
        values: Map<String, String>,
    ): InstallCampaign? {
        val labels = listOfNotNull(source, values[KEY_MEDIUM], values[KEY_CAMPAIGN], values[KEY_CONTENT])
        if (!labels.all { PROVIDER_VALUE.matches(it) }) return null
        val campaignId = values[KEY_ID]
        if (campaignId != null && !isCampaignId(campaignId)) return null
        return InstallCampaign(
            source = source,
            medium = values[KEY_MEDIUM],
            campaign = values[KEY_CAMPAIGN],
            content = values[KEY_CONTENT],
            campaignId = campaignId,
        )
    }

    /** `<experiment_id>_<yyyymmdd>` — 날짜는 실제로 있는 달력 날짜여야 한다. */
    private fun isManualCampaign(value: String): Boolean {
        val match = MANUAL_CAMPAIGN.matchEntire(value) ?: return false
        val (experimentId, date) = match.destructured
        if (experimentId.length > ID_MAX_LENGTH) return false
        return try {
            LocalDate.parse(date, CAMPAIGN_DATE)
            true
        } catch (error: DateTimeParseException) {
            false
        }
    }

    private fun isId(value: String): Boolean = value.length <= ID_MAX_LENGTH && ID.matches(value)

    private fun isCampaignId(value: String): Boolean = CAMPAIGN_ID.matches(value)

    /** 잘못된 `%` 인코딩이면 null. `Charset` 을 받는 오버로드는 API 33 부터라 이름으로 넘긴다. */
    private fun String.decodeOrNull(): String? =
        try {
            URLDecoder.decode(this, Charsets.UTF_8.name())
        } catch (error: IllegalArgumentException) {
            null
        }

    private const val KEY_SOURCE = "utm_source"
    private const val KEY_MEDIUM = "utm_medium"
    private const val KEY_CAMPAIGN = "utm_campaign"
    private const val KEY_CONTENT = "utm_content"
    private const val KEY_ID = "utm_id"
    private val ALLOWED_KEYS = setOf(KEY_SOURCE, KEY_MEDIUM, KEY_CAMPAIGN, KEY_CONTENT, KEY_ID)

    /** Play 가 값을 모를 때 채우는 표기. decode 한 뒤의 모양이다(`(not%20set)`·`(not+set)` 모두 이것이 된다). */
    private const val NOT_SET = "(not set)"

    /** 넓힐 때는 04 문서 · 실험 레지스트리 · 링크 생성기와 함께 고친다(04 §2.1). */
    private val MANUAL_SOURCES = setOf("meta")
    private val MANUAL_MEDIUMS = setOf("paid")

    private const val ID_MAX_LENGTH = 32
    private const val ID_PATTERN = "[a-z][a-z0-9]*(?:_[a-z0-9]+)*"
    private val ID = Regex(ID_PATTERN)
    private val MANUAL_CAMPAIGN = Regex("($ID_PATTERN)_([0-9]{8})")
    private val CAMPAIGN_DATE = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT)
    private val CAMPAIGN_ID = Regex("[A-Za-z0-9]{1,100}")
    private val PROVIDER_VALUE = Regex("[a-z0-9_.-]{1,36}")

    private val NO_CAMPAIGN = InstallAttribution(InstallReferrerStatus.NO_CAMPAIGN)
    private val INVALID = InstallAttribution(InstallReferrerStatus.INVALID)
}
