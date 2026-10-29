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

    @Test
    fun `장소 필드가 없는 구서버 응답은 장소 없이 읽힌다`() {
        val event = decode("")

        assertNull(event.place)
        assertNull(event.address)
        assertNull(event.latitude)
        assertNull(event.longitude)
        assertEquals(emptyList<String>(), event.placeCandidates)
    }

    @Test
    fun `장소 후보는 서버 순서대로 이름만 도메인에 전달한다`() {
        val event =
            decode(
                ""","subtitle":"{place}에서 친구와 파스타","place":"성수동 카페","address":"서울 성동구 아차산로9길 8",""" +
                    """"latitude":37.5445,"longitude":127.0559,""" +
                    """"placeCandidates":[{"name":"성수동 카페"},{"name":"△△공원"},{"name":"어니언 성수"}]""",
            )

        assertEquals("{place}에서 친구와 파스타", event.subtitle)
        assertEquals("성수동 카페", event.place)
        assertEquals("서울 성동구 아차산로9길 8", event.address)
        assertEquals(37.5445, event.latitude!!, 0.0)
        assertEquals(127.0559, event.longitude!!, 0.0)
        assertEquals(listOf("성수동 카페", "△△공원", "어니언 성수"), event.placeCandidates)
    }

    @Test
    fun `좌표가 한쪽만 오면 둘 다 버린다`() {
        val event = decode(""","latitude":37.5445""")

        assertNull(event.latitude)
        assertNull(event.longitude)
    }

    @Test
    fun `공백 후보는 버린다`() {
        val event = decode(""","placeCandidates":[{"name":" "},{"name":"△△공원"}]""")

        assertEquals(listOf("△△공원"), event.placeCandidates)
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
