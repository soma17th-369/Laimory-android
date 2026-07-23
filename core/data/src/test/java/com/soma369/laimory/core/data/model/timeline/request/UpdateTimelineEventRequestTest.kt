package com.soma369.laimory.core.data.model.timeline.request

import com.soma369.laimory.core.domain.model.timeline.TimelineEventPhotoAddition
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineEventUpdateField
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDateTime

class UpdateTimelineEventRequestTest {
    @Test
    fun `필수 4키는 nullable 값과 초가 0인 시각까지 exact shape으로 전송한다`() {
        val request =
            command(
                subtitle = null,
                startAt = LocalDateTime.of(2026, 7, 8, 14, 0),
                endAt = null,
            ).toRequestJson()

        assertEquals(
            Json.parseToJsonElement(
                """
                {
                  "title":"카페에서 휴식",
                  "subtitle":null,
                  "startAt":"2026-07-08T14:00:00",
                  "endAt":null
                }
                """.trimIndent(),
            ),
            request,
        )
        assertFalse(request.containsKey("eventType"))
        assertFalse(request.containsKey("memo"))
        assertFalse(request.containsKey("photosToAdd"))
    }

    @Test
    fun `optional 필드는 키 누락과 명시적 값 또는 null을 구분한다`() {
        val request =
            command(
                eventType = TimelineEventType.MEAL,
                memo = TimelineEventUpdateField.Value(null),
                photosToAdd = TimelineEventUpdateField.Value(emptyList()),
            ).toRequestJson()

        assertEquals("MEAL", request["eventType"]?.jsonPrimitive?.content)
        assertEquals("null", request["memo"].toString())
        assertEquals("[]", request["photosToAdd"].toString())
    }

    @Test
    fun `memo Value 원문은 가공하지 않고 전송한다`() {
        val memo = "  원문 메모  "

        val request =
            command(
                memo = TimelineEventUpdateField.Value(memo),
            ).toRequestJson()

        assertEquals(memo, request["memo"]?.jsonPrimitive?.content)
    }

    @Test
    fun `PHOTO 추가는 중첩 payload와 nullable 좌표를 서버 shape으로 전송한다`() {
        val request =
            command(
                photosToAdd =
                    TimelineEventUpdateField.Value(
                        listOf(
                            TimelineEventPhotoAddition(
                                rawId = "raw-photo-1",
                                startAt = LocalDateTime.of(2026, 7, 8, 14, 5),
                                endAt = null,
                                filename = "server-photo.jpg",
                                clientPhotoUri = "content://media/1",
                                latitude = 37.5665,
                                longitude = null,
                            ),
                        ),
                    ),
            ).toRequestJson()

        assertEquals(
            Json.parseToJsonElement(
                """
                {
                  "title":"카페에서 휴식",
                  "subtitle":"성수동",
                  "startAt":"2026-07-08T14:00:00",
                  "endAt":"2026-07-08T15:30:00",
                  "photosToAdd":[{
                    "rawId":"raw-photo-1",
                    "startAt":"2026-07-08T14:05:00",
                    "payload":{
                      "filename":"server-photo.jpg",
                      "clientPhotoUri":"content://media/1",
                      "latitude":37.5665
                    }
                  }]
                }
                """.trimIndent(),
            ),
            request,
        )
    }

    private fun command(
        subtitle: String? = "성수동",
        startAt: LocalDateTime = LocalDateTime.of(2026, 7, 8, 14, 0),
        endAt: LocalDateTime? = LocalDateTime.of(2026, 7, 8, 15, 30),
        eventType: TimelineEventType? = null,
        memo: TimelineEventUpdateField<String?> = TimelineEventUpdateField.Unchanged,
        photosToAdd: TimelineEventUpdateField<List<TimelineEventPhotoAddition>> = TimelineEventUpdateField.Unchanged,
    ) = UpdateTimelineEventCommand(
        timelineEventId = 7L,
        title = "카페에서 휴식",
        subtitle = subtitle,
        startAt = startAt,
        endAt = endAt,
        eventType = eventType,
        memo = memo,
        photosToAdd = photosToAdd,
    )
}
