package com.soma369.laimory.core.data.model.timeline.request

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalSerializationApi::class)
class DraftSourceItemProjectionTest {
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")

    /** 2026-07-08 21:30:00 KST */
    private val start: Instant = LocalDateTime.of(2026, 7, 8, 21, 30, 0).atZone(zone).toInstant()

    // 실제 앱 Json 과 동일하게 null 필드는 생략되도록 explicitNulls=false.
    private val json = Json { explicitNulls = false }

    private fun item(
        payload: SourceItemPayload,
        end: Instant? = null,
    ): SourceItem =
        SourceItem(
            rawId = "raw-1",
            startAt = start,
            endAt = end,
            timeZoneId = zone,
            payload = payload,
            sourceName = SourceName.MEDIA_STORE,
            sourceKey = "key-1",
            collectedAt = start,
        )

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    @Test
    fun `PHOTO 의 filename 은 업로드 결과로 채워지고 clientPhotoUri 는 유지된다`() {
        val dto =
            item(
                PhotoPayload(
                    fileName = "local_display.jpg",
                    clientPhotoUri = "content://media/1001",
                    latitude = 37.5,
                    longitude = 127.0,
                    description = null,
                ),
            ).toSourceItemDto(json, uploadedPhotoFilename = "server-abc.jpg")

        assertEquals("PHOTO", dto.itemType)
        assertEquals("raw-1", dto.rawId)
        val p = dto.payload
        assertEquals("server-abc.jpg", p.str("filename"))
        assertEquals("content://media/1001", p.str("clientPhotoUri"))
        assertEquals("37.5", p.str("latitude"))
        // description(null)·서버 readOnly photoUrl 은 요청에 넣지 않는다.
        assertFalse("description(null)은 생략", p.containsKey("description"))
        assertFalse("photoUrl 은 서버 파생이라 미전송", p.containsKey("photoUrl"))
    }

    @Test
    fun `PHOTO 업로드 파일명이 없으면 실패시킨다`() {
        val e =
            runCatching {
                item(PhotoPayload("f.jpg", "content://f", null, null, null))
                    .toSourceItemDto(json, uploadedPhotoFilename = null)
            }.exceptionOrNull()
        assertTrue(e is ApiException)
    }

    @Test
    fun `startAt 은 시간대 로컬 datetime(오프셋 없음), endAt 없으면 null`() {
        val dto =
            item(PhotoPayload("f.jpg", "content://f", null, null, null))
                .toSourceItemDto(json, "s.jpg")
        assertEquals("2026-07-08T21:30", dto.startAt)
        assertNull(dto.endAt)
    }

    @Test
    fun `HEALTH STEPS 는 표시 문자열 value 로 보낸다`() {
        val dto =
            item(HealthPayload(HealthPayload.Metric.STEPS, 8421.0, "step"))
                .toSourceItemDto(json, null)
        assertEquals("STEPS", dto.payload.str("metric"))
        assertEquals("8421보", dto.payload.str("value"))
    }

    @Test
    fun `MOVEMENT 은 start·end 좌표와 distanceMeters 를 유지한다`() {
        val dto =
            item(
                MovementPayload(
                    start = GeoPoint(37.1538856, 127.0781832),
                    end = GeoPoint(37.532292, 126.9625982),
                    distanceMeters = 58137.2,
                    transports = MovementPayload.Transport.IN_VEHICLE,
                ),
            ).toSourceItemDto(json, null)

        assertEquals("MOVEMENT", dto.itemType)
        val p = dto.payload
        val startPoint = p["start"]!!.jsonObject
        assertEquals("37.1538856", startPoint.str("latitude"))
        assertEquals("127.0781832", startPoint.str("longitude"))
        val endPoint = p["end"]!!.jsonObject
        assertEquals("37.532292", endPoint.str("latitude"))
        assertEquals("126.9625982", endPoint.str("longitude"))
        assertEquals("58137.2", p.str("distanceMeters"))
        assertEquals("IN_VEHICLE", p.str("transports"))
    }

    @Test
    fun `NOTIFICATION 은 packageName·collectReason 을 보내지 않는다`() {
        val dto =
            item(
                NotificationPayload(
                    appName = "카톡",
                    packageName = "com.kakao.talk",
                    title = "제목",
                    text = "본문",
                    collectReason = NotificationPayload.CollectReason.ALL,
                ),
            ).toSourceItemDto(json, null)
        val p = dto.payload
        assertEquals("카톡", p.str("appName"))
        assertFalse("packageName 은 서버 projection 에서 제외", p.containsKey("packageName"))
        assertFalse("collectReason 은 로컬 전용", p.containsKey("collectReason"))
    }
}
