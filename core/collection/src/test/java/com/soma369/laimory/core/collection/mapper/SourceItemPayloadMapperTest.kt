package com.soma369.laimory.core.collection.mapper

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.LocationPayload
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceItemPayloadMapperTest {
    private fun roundTrip(payload: SourceItemPayload): SourceItemPayload {
        val json = SourceItemPayloadMapper.toJson(payload)
        return SourceItemPayloadMapper.fromJson(payload.itemType, json)
    }

    @Test
    fun `LOCATION payload 라운드트립`() {
        val payload = LocationPayload(latitude = 37.1538856, longitude = 127.0781832)

        assertEquals(payload, roundTrip(payload))
    }

    @Test
    fun `MOVEMENT payload 라운드트립`() {
        val payload =
            MovementPayload(
                start = GeoPoint(latitude = 37.1538856, longitude = 127.0781832),
                end = GeoPoint(latitude = 37.16312, longitude = 127.08514),
                distanceMeters = 1352.4,
                transport = MovementPayload.Transport.WALKING,
            )

        assertEquals(payload, roundTrip(payload))
    }

    @Test
    fun `CALENDAR payload 라운드트립`() {
        val payload =
            CalendarPayload(
                title = "ASM 발대식",
                description = "AI·SW마에스트로 제17기 발대식",
                locationText = "용산 서울드래곤시티",
                allDay = false,
            )

        assertEquals(payload, roundTrip(payload))
    }

    @Test
    fun `CALENDAR payload 는 null 필드를 보존한다`() {
        val payload =
            CalendarPayload(
                title = "제목만 있는 일정",
                description = null,
                locationText = null,
                allDay = true,
            )

        assertEquals(payload, roundTrip(payload))
    }

    @Test
    fun `HEALTH payload 라운드트립`() {
        val payload = HealthPayload(metric = HealthPayload.Metric.STEPS, value = 10145.0, unit = "보")

        assertEquals(payload, roundTrip(payload))
    }

    @Test
    fun `NOTIFICATION payload 라운드트립`() {
        val payload =
            NotificationPayload(
                appName = "카카오톡",
                title = "라이모리",
                text = "내일 모임 10시로 변경됐어요",
            )

        assertEquals(payload, roundTrip(payload))
    }

    @Test
    fun `PHOTO payload 라운드트립`() {
        val payload =
            PhotoPayload(
                fileName = "0197b1c2.jpg",
                clientPhotoUri = "content://media/external/images/media/1000010519",
                latitude = null,
                longitude = null,
                description = null,
            )

        assertEquals(payload, roundTrip(payload))
    }

    @Test
    fun `PHOTO payload JSON 은 업로드 포맷 필드명 filename 을 사용한다`() {
        val payload =
            PhotoPayload(
                fileName = "0197b1c2.jpg",
                clientPhotoUri = "content://media/external/images/media/1000010519",
                latitude = null,
                longitude = null,
                description = null,
            )

        val keys = Json.parseToJsonElement(SourceItemPayloadMapper.toJson(payload)).jsonObject.keys

        assertTrue("filename" in keys)
        assertTrue("clientPhotoUri" in keys)
    }

    @Test
    fun `이후 추가된 payload 필드는 무시하고 읽을 수 있다`() {
        val jsonWithUnknownField =
            """{"latitude":37.1538856,"longitude":127.0781832,"accuracyMeters":12.5}"""

        val payload =
            SourceItemPayloadMapper.fromJson(
                itemType = ItemType.LOCATION,
                payloadJson = jsonWithUnknownField,
            )

        assertEquals(LocationPayload(latitude = 37.1538856, longitude = 127.0781832), payload)
    }
}
