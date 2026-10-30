package com.soma369.laimory.core.domain.model.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class TimelineEventSubtitleTest {
    @Test
    fun `장소 자리가 없으면 부제를 그대로 보여 준다`() {
        assertEquals("강남역 → 성수역 · 2호선", event(subtitle = "강남역 → 성수역 · 2호선").displaySubtitle)
        assertNull(event(subtitle = null).displaySubtitle)
    }

    @Test
    fun `장소 자리를 지금 장소로 채운다`() {
        val event = event(subtitle = "{place}에서 친구와 파스타", place = "△△공원")

        assertEquals("△△공원에서 친구와 파스타", event.displaySubtitle)
    }

    @Test
    fun `장소 자리가 여러 개면 모두 채운다`() {
        val event = event(subtitle = "{place} 앞에서 만나 {place} 안으로", place = "성수역")

        assertEquals("성수역 앞에서 만나 성수역 안으로", event.displaySubtitle)
    }

    @Test
    fun `장소가 비었으면 주소로 채운다`() {
        val event = event(subtitle = "{place}에서 업무", place = " ", address = "서울 성동구 성수이로 113")

        assertEquals("서울 성동구 성수이로 113에서 업무", event.displaySubtitle)
    }

    @Test
    fun `장소도 주소도 없으면 자리 표시를 보이지 않는다`() {
        assertEquals("점심은", event(subtitle = "점심은 {place}").displaySubtitle)
    }

    @Test
    fun `장소 자리마다 나누고 모든 자리가 같은 장소를 가리킨다`() {
        val event = event(subtitle = "친구와 {place}에서 파스타, 다시 {place}", place = "어니언 성수")

        assertEquals(
            PlaceSubtitle(texts = listOf("친구와 ", "에서 파스타, 다시 ", ""), place = "어니언 성수"),
            event.placeSubtitle(),
        )
        assertEquals("친구와 어니언 성수에서 파스타, 다시 어니언 성수", event.placeSubtitle()?.text)
    }

    @Test
    fun `장소 자리가 없거나 장소가 비었으면 나누지 않는다`() {
        assertNull(event(subtitle = "어니언 성수에서 파스타", place = "어니언 성수").placeSubtitle())
        assertNull(event(subtitle = null, place = "어니언 성수").placeSubtitle())
        assertNull(event(subtitle = "{place}에서 파스타", address = "서울 성동구 아차산로9길 8").placeSubtitle())
    }

    private fun event(
        subtitle: String?,
        place: String? = null,
        address: String? = null,
    ) = TimelineEvent(
        timelineEventId = 1L,
        eventType = TimelineEventType.MEAL,
        startAt = LocalDateTime.of(2026, 10, 30, 12, 20),
        endAt = null,
        title = "점심 식사",
        subtitle = subtitle,
        memo = null,
        question = null,
        items = emptyList(),
        place = place,
        address = address,
    )
}
