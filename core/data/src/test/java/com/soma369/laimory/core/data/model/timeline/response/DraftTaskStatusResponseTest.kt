package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.timeline.DraftTaskFailureReason
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatus
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DraftTaskStatusResponseTest {
    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

    @Test
    fun `PROCESSING은 optional elapsedSeconds를 보존한다`() {
        val snapshot = decode("""{"status":"PROCESSING","elapsedSeconds":12}""")

        assertEquals(DraftTaskStatus.PROCESSING, snapshot.status)
        assertEquals(12L, snapshot.elapsedSeconds)
        assertNull(snapshot.result)
        assertNull(snapshot.failure)
    }

    @Test
    fun `legacy PROCESSING은 elapsedSeconds 누락을 null로 보존한다`() {
        val snapshot = decode("""{"status":"PROCESSING"}""")

        assertEquals(DraftTaskStatus.PROCESSING, snapshot.status)
        assertNull(snapshot.elapsedSeconds)
    }

    @Test
    fun `SUCCESS는 DailyTimeline과 Event Item 식별자를 Domain까지 매핑한다`() {
        val snapshot = decode(SUCCESS_JSON)
        val timeline = requireNotNull(snapshot.result)
        val event = timeline.events.single()
        val item = event.items.single()

        assertEquals(DraftTaskStatus.SUCCESS, snapshot.status)
        assertEquals(31L, timeline.dailyRecordId)
        assertEquals(LocalDate.of(2026, 7, 8), timeline.recordDate)
        assertEquals(TimelineEmotion.HAPPY, timeline.emotion)
        assertEquals(41L, event.timelineEventId)
        assertEquals(TimelineEventType.MEAL, event.eventType)
        assertEquals(LocalDateTime.of(2026, 7, 8, 12, 0), event.startAt)
        assertNull(event.endAt)
        assertEquals(51L, item.timelineItemId)
        assertEquals(TimelineItemType.PHOTO, item.itemType)
        assertEquals("photo-raw-id", item.rawId)
        assertEquals("https://example.com/photo.jpg", item.photoUrl)
    }

    @Test
    fun `표시 literal 미지 값은 UNKNOWN으로 fallback한다`() {
        val snapshot =
            decode(
                SUCCESS_JSON
                    .replace("\"HAPPY\"", "\"EXCITED\"")
                    .replace("\"MEAL\"", "\"FUTURE_EVENT\"")
                    .replace("\"PHOTO\"", "\"FUTURE_ITEM\""),
            )
        val timeline = requireNotNull(snapshot.result)

        assertEquals(TimelineEmotion.UNKNOWN, timeline.emotion)
        assertEquals(TimelineEventType.UNKNOWN, timeline.events.single().eventType)
        assertEquals(TimelineItemType.UNKNOWN, timeline.events.single().items.single().itemType)
    }

    @Test
    fun `FAILED는 알려진 코드와 미지 코드를 타입으로 구분한다`() {
        val known = decode("""{"status":"FAILED","error":"ERROR_1009"}""")
        val unknown = decode("""{"status":"FAILED","error":"ERROR_1999"}""")

        assertEquals(DraftTaskFailureReason.AI_DISPATCH_FAILURE, known.failure)
        assertEquals(DraftTaskFailureReason.UNKNOWN, unknown.failure)
    }

    @Test
    fun `status 미지 값과 terminal shape 위반은 오류다`() {
        listOf(
            """{"status":"WAITING"}""",
            """{"status":"PROCESSING","elapsedSeconds":-1}""",
            """{"status":"PROCESSING","error":"ERROR_1009"}""",
            """{"status":"SUCCESS"}""",
            """{"status":"SUCCESS","result":${SUCCESS_RESULT},"elapsedSeconds":1}""",
            """{"status":"FAILED"}""",
            """{"status":"FAILED","error":"ERROR_1009","result":${SUCCESS_RESULT}}""",
        ).forEach { raw ->
            assertThrows("잘못된 terminal shape: $raw", ApiException.UnknownException::class.java) { decode(raw) }
        }
    }

    private fun decode(raw: String) = json.decodeFromString<DraftTaskStatusResponse>(raw).toDomain()

    private companion object {
        const val SUCCESS_RESULT =
            """{
              "dailyRecordId":31,
              "recordDate":"2026-07-08",
              "emotionType":"HAPPY",
              "events":[{
                "timelineEventId":41,
                "eventType":"MEAL",
                "startAt":"2026-07-08T12:00:00",
                "endAt":null,
                "title":"점심",
                "subtitle":null,
                "memo":null,
                "items":[{
                  "timelineItemId":51,
                  "itemType":"PHOTO",
                  "rawId":"photo-raw-id",
                  "startAt":"2026-07-08T12:00:00",
                  "endAt":null,
                  "payload":{"photoUrl":"https://example.com/photo.jpg","futureField":true}
                }]
              }]
            }"""

        const val SUCCESS_JSON = """{"status":"SUCCESS","result":$SUCCESS_RESULT}"""
    }
}
