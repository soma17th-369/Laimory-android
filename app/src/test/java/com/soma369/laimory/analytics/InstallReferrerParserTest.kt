package com.soma369.laimory.analytics

import com.soma369.laimory.core.domain.model.analytics.InstallCampaign
import com.soma369.laimory.core.domain.model.analytics.InstallReferrerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstallReferrerParserTest {
    private fun parse(referrer: String?) = InstallReferrerParser.parse(referrer)

    @Test
    fun `Meta 수동 캠페인 값을 받는다`() {
        val result =
            parse("utm_source=meta&utm_medium=paid&utm_campaign=sleep_hook_20260924&utm_content=video_a&utm_id=120212345")

        assertEquals(InstallReferrerStatus.PARSED, result.status)
        assertEquals(
            InstallCampaign(
                source = "meta",
                medium = "paid",
                campaign = "sleep_hook_20260924",
                content = "video_a",
                campaignId = "120212345",
            ),
            result.campaign,
        )
    }

    @Test
    fun `content 와 utm_id 는 없어도 된다`() {
        val result = parse("utm_source=meta&utm_medium=paid&utm_campaign=exp1_20260101")

        assertEquals(InstallReferrerStatus.PARSED, result.status)
        assertNull(result.campaign?.content)
        assertNull(result.campaign?.campaignId)
    }

    @Test
    fun `API 가 준 문자열을 한 번만 decode 한다`() {
        // 한 번 풀면 `exp1_2026010%31` 로 형식 위반이다. 두 번 풀었다면 `exp1_20260101` 로 통과했을 것이다.
        assertEquals(InstallReferrerStatus.INVALID, parse("utm_source=meta&utm_medium=paid&utm_campaign=exp1_2026010%2531").status)
        // 인코딩된 구분자는 값의 일부다.
        assertEquals(
            InstallReferrerStatus.PARSED,
            parse("utm_source=meta&utm_medium=paid&utm_campaign=exp%5F1_20260101").status,
        )
    }

    @Test
    fun `Play 가 준 공급자 값은 형식만 보고 그대로 보존한다`() {
        val result = parse("utm_source=google-play&utm_medium=organic")

        assertEquals(InstallReferrerStatus.PROVIDER, result.status)
        assertEquals(InstallCampaign(source = "google-play", medium = "organic"), result.campaign)
    }

    @Test
    fun `공급자 값에 대문자가 섞이면 바꾸지 않고 거절한다`() {
        assertEquals(InstallReferrerStatus.INVALID, parse("utm_source=Google-Play&utm_medium=organic").status)
    }

    @Test
    fun `허용 키 밖의 값은 버린다`() {
        val result = parse("utm_source=google&utm_medium=cpc&gclid=abc123&utm_term=sleep")

        assertEquals(InstallReferrerStatus.PROVIDER, result.status)
        assertEquals(InstallCampaign(source = "google", medium = "cpc"), result.campaign)
    }

    @Test
    fun `UTM 이 없으면 캠페인 없음이다`() {
        assertEquals(InstallReferrerStatus.NO_CAMPAIGN, parse(null).status)
        assertEquals(InstallReferrerStatus.NO_CAMPAIGN, parse("").status)
        assertEquals(InstallReferrerStatus.NO_CAMPAIGN, parse("gclid=abc123").status)
    }

    @Test
    fun `같은 키가 두 번 오면 거절한다`() {
        assertEquals(
            InstallReferrerStatus.INVALID,
            parse("utm_source=meta&utm_source=meta&utm_medium=paid&utm_campaign=exp1_20260101").status,
        )
    }

    @Test
    fun `잘못된 인코딩은 거절한다`() {
        assertEquals(InstallReferrerStatus.INVALID, parse("utm_source=meta&utm_medium=paid&utm_campaign=exp1_2026%ZZ").status)
    }

    @Test
    fun `Meta 인데 medium 이나 campaign 이 없으면 거절한다`() {
        assertEquals(InstallReferrerStatus.INVALID, parse("utm_source=meta&utm_campaign=exp1_20260101").status)
        assertEquals(InstallReferrerStatus.INVALID, parse("utm_source=meta&utm_medium=paid").status)
    }

    @Test
    fun `source 없이 다른 UTM 만 오면 거절한다`() {
        assertEquals(InstallReferrerStatus.INVALID, parse("utm_medium=paid&utm_campaign=exp1_20260101").status)
    }

    @Test
    fun `Meta 의 medium 은 허용 목록만 받는다`() {
        assertEquals(InstallReferrerStatus.INVALID, parse("utm_source=meta&utm_medium=cpc&utm_campaign=exp1_20260101").status)
    }

    @Test
    fun `campaign 날짜는 실제 달력 날짜여야 한다`() {
        assertEquals(InstallReferrerStatus.INVALID, parse("utm_source=meta&utm_medium=paid&utm_campaign=exp1_20260230").status)
        assertEquals(InstallReferrerStatus.PARSED, parse("utm_source=meta&utm_medium=paid&utm_campaign=exp1_20280229").status)
    }

    @Test
    fun `campaign 의 experiment_id 형식과 길이를 본다`() {
        val id32 = "a".repeat(32)
        val id33 = "a".repeat(33)
        assertEquals(InstallReferrerStatus.PARSED, parse("utm_source=meta&utm_medium=paid&utm_campaign=${id32}_20260101").status)
        assertEquals(InstallReferrerStatus.INVALID, parse("utm_source=meta&utm_medium=paid&utm_campaign=${id33}_20260101").status)
        assertEquals(InstallReferrerStatus.INVALID, parse("utm_source=meta&utm_medium=paid&utm_campaign=1exp_20260101").status)
        assertEquals(InstallReferrerStatus.INVALID, parse("utm_source=meta&utm_medium=paid&utm_campaign=Exp_20260101").status)
        assertEquals(InstallReferrerStatus.INVALID, parse("utm_source=meta&utm_medium=paid&utm_campaign=exp1").status)
    }

    @Test
    fun `content 와 utm_id 형식을 본다`() {
        val base = "utm_source=meta&utm_medium=paid&utm_campaign=exp1_20260101"
        assertEquals(InstallReferrerStatus.INVALID, parse("$base&utm_content=Video").status)
        assertEquals(InstallReferrerStatus.INVALID, parse("$base&utm_content=${"a".repeat(33)}").status)
        assertEquals(InstallReferrerStatus.INVALID, parse("$base&utm_id=12-34").status)
        assertEquals(InstallReferrerStatus.INVALID, parse("$base&utm_id=${"1".repeat(101)}").status)
        assertEquals(InstallReferrerStatus.PARSED, parse("$base&utm_id=${"1".repeat(100)}").status)
    }

    @Test
    fun `Meta 대문자 source 는 공급자 값으로도 받지 않는다`() {
        assertEquals(InstallReferrerStatus.INVALID, parse("utm_source=Meta&utm_medium=paid&utm_campaign=exp1_20260101").status)
    }
}
