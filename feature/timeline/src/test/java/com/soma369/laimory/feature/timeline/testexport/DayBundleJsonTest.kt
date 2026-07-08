package com.soma369.laimory.feature.timeline.testexport

import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DayBundleJsonTest {
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val date: LocalDate = LocalDate.of(2026, 7, 8)

    private fun item(
        payload: SourceItemPayload,
        rawId: String,
    ): SourceItem =
        SourceItem(
            rawId = rawId,
            startAt = Instant.parse("2026-07-08T04:00:00Z"),
            endAt = null,
            timeZoneId = zone,
            payload = payload,
            sourceName = SourceName.MEDIA_STORE,
            sourceKey = "key-$rawId",
            collectedAt = Instant.parse("2026-07-08T04:00:00Z"),
        )

    @Test
    fun `recordDate·recordTimeZone·sourceItems 절단면을 만든다`() {
        val photo = item(PhotoPayload("a.jpg", "content://a", 37.0, 127.0, null), "r1")
        val steps = item(HealthPayload(HealthPayload.Metric.STEPS, 1000.0, "count"), "r2")

        val json =
            JSONObject(
                DayBundleJson.build(date, zone, listOf(photo, steps)) { if (it.rawId == "r1") "000_a.jpg" else null },
            )

        assertEquals("2026-07-08", json.getString("recordDate"))
        assertEquals("Asia/Seoul", json.getString("recordTimeZone"))
        assertEquals(2, json.getJSONArray("sourceItems").length())
    }

    @Test
    fun `아이템은 rawId·itemType·로컬 startAt 을 담고 sourceKey 는 빠진다`() {
        val photo = item(PhotoPayload("a.jpg", "content://a", null, null, null), "r1")

        val item0 =
            JSONObject(DayBundleJson.build(date, zone, listOf(photo)) { "000_a.jpg" })
                .getJSONArray("sourceItems")
                .getJSONObject(0)

        assertEquals("r1", item0.getString("rawId"))
        assertEquals("PHOTO", item0.getString("itemType"))
        // 04:00Z → Asia/Seoul 13:00 로컬(오프셋 없음)
        assertEquals("2026-07-08T13:00", item0.getString("startAt"))
        assertTrue(item0.isNull("endAt"))
        assertFalse(item0.has("sourceKey"))
        assertFalse(item0.has("sourceName"))

        val payload = item0.getJSONObject("payload")
        assertEquals("a.jpg", payload.getString("fileName"))
        assertEquals("000_a.jpg", payload.getString("photoFile"))
    }

    @Test
    fun `HEALTH 는 걸음수·수면을 표시 문자열로 보낸다`() {
        val steps = item(HealthPayload(HealthPayload.Metric.STEPS, 8421.0, "count"), "r1")
        val sleep = item(HealthPayload(HealthPayload.Metric.SLEEP, 480.0, "minute"), "r2")

        val arr =
            JSONObject(DayBundleJson.build(date, zone, listOf(steps, sleep)) { null })
                .getJSONArray("sourceItems")

        assertEquals("8421보", arr.getJSONObject(0).getJSONObject("payload").getString("value"))
        assertEquals("480분", arr.getJSONObject(1).getJSONObject("payload").getString("value"))
    }

    @Test
    fun `빈 목록이면 sourceItems 가 비어 있다`() {
        val json = JSONObject(DayBundleJson.build(date, zone, emptyList()) { null })

        assertEquals(0, json.getJSONArray("sourceItems").length())
        assertTrue(json.getString("recordTimeZone").isNotEmpty())
    }
}
