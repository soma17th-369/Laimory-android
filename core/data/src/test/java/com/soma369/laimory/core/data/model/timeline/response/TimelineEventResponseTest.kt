package com.soma369.laimory.core.data.model.timeline.response

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineEventResponseTest {
    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

    @Test
    fun `question 값이 있으면 도메인까지 전달한다`() {
        val event = decode(""","question":"오늘 누구와 함께였나요?"""")

        assertEquals("오늘 누구와 함께였나요?", event.question)
    }

    @Test
    fun `question 필드가 누락되면 null이다`() {
        // question 미배포 서버 응답과의 하위 호환.
        assertNull(decode("").question)
    }

    @Test
    fun `question이 명시적 null이면 null이다`() {
        assertNull(decode(""","question":null""").question)
    }

    @Test
    fun `question은 다른 필드 매핑에 영향을 주지 않는다`() {
        val event = decode(""","question":"무엇을 먹었나요?"""")

        assertEquals(41L, event.timelineEventId)
        assertEquals("점심", event.title)
        assertNull(event.memo)
    }

    private fun decode(questionJson: String) =
        json
            .decodeFromString<TimelineEventResponse>(
                """
                {
                  "timelineEventId":41,
                  "eventType":"MEAL",
                  "startAt":"2026-07-28T12:00:00",
                  "title":"점심",
                  "items":[]$questionJson
                }
                """.trimIndent(),
            ).toDomain()
}
