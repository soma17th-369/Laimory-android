package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DailyTimelineListResponseTest {
    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

    @Test
    fun `빈 timelines는 빈 Domain 목록으로 매핑한다`() {
        val timelines = json.decodeFromString<DailyTimelineListResponse>("""{"timelines":[]}""").toDomain()

        assertTrue(timelines.isEmpty())
    }

    @Test
    fun `복수 DailyRecord graph를 서버 순서 그대로 Domain까지 매핑한다`() {
        val timelines = json.decodeFromString<DailyTimelineListResponse>(LIST_JSON).toDomain()

        assertEquals(listOf(32L, 31L), timelines.map { it.dailyRecordId })
        val latest = timelines.first()
        val event = latest.events.single()
        val item = event.items.single()
        assertEquals(LocalDate.of(2026, 7, 28), latest.recordDate)
        assertEquals(TimelineEmotion.HAPPY, latest.emotion)
        assertEquals(41L, event.timelineEventId)
        assertEquals(TimelineEventType.MEAL, event.eventType)
        assertEquals(LocalDateTime.of(2026, 7, 28, 12, 0), event.startAt)
        assertEquals(51L, item.timelineItemId)
        assertEquals(TimelineItemType.PHOTO, item.itemType)
        assertEquals("photo-raw-id", item.rawId)
        assertEquals("https://example.com/photo.jpg", item.photoUrl)
    }

    @Test
    fun `emotionType과 Event가 없는 DailyRecord도 목록에서 제외하지 않는다`() {
        val timelines = json.decodeFromString<DailyTimelineListResponse>(LIST_JSON).toDomain()

        val emptyRecord = timelines.last()
        assertEquals(31L, emptyRecord.dailyRecordId)
        assertEquals(LocalDate.of(2026, 7, 27), emptyRecord.recordDate)
        assertNull(emptyRecord.emotion)
        assertTrue(emptyRecord.events.isEmpty())
    }

    private companion object {
        val LIST_JSON =
            """
            {
              "timelines":[
                {
                  "dailyRecordId":32,
                  "recordDate":"2026-07-28",
                  "emotionType":"HAPPY",
                  "events":[
                    {
                      "timelineEventId":41,
                      "eventType":"MEAL",
                      "startAt":"2026-07-28T12:00:00",
                      "endAt":null,
                      "title":"점심",
                      "subtitle":null,
                      "memo":null,
                      "items":[
                        {
                          "timelineItemId":51,
                          "itemType":"PHOTO",
                          "rawId":"photo-raw-id",
                          "startAt":null,
                          "endAt":null,
                          "payload":{"photoUrl":"https://example.com/photo.jpg"}
                        }
                      ]
                    }
                  ]
                },
                {
                  "dailyRecordId":31,
                  "recordDate":"2026-07-27",
                  "events":[]
                }
              ]
            }
            """.trimIndent()
    }
}
