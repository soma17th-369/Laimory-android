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

    @Test
    fun `고친 부제에 장소 이름이 통째로 남아 있으면 첫 자리를 장소 자리로 되돌린다`() {
        assertEquals("오늘 {place}에서 저녁을 먹었어요.", "오늘 라쿤 피자에서 저녁을 먹었어요.".restorePlacePlaceholder("라쿤 피자"))
        assertEquals("{place} 갔다가 라쿤 피자", "라쿤 피자 갔다가 라쿤 피자".restorePlacePlaceholder("라쿤 피자"))
    }

    @Test
    fun `장소 이름 일부만 남았거나 장소가 비었으면 그대로 둔다`() {
        assertEquals("라쿤 에서 저녁을 먹었어요.", "라쿤 에서 저녁을 먹었어요.".restorePlacePlaceholder("라쿤 피자"))
        assertEquals("저녁을 먹었어요.", "저녁을 먹었어요.".restorePlacePlaceholder(" "))
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
