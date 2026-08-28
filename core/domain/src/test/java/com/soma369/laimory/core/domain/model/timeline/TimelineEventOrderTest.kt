package com.soma369.laimory.core.domain.model.timeline

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class TimelineEventOrderTest {
    @Test
    fun `시작 시각 오름차순으로 정렬한다`() {
        // 서버가 순서를 보장하지 않아 응답 배열이 뒤섞여 올 수 있다.
        val sorted = listOf(event(3, 15, 0), event(1, 8, 30), event(2, 12, 0)).sortedForDisplay()

        assertEquals(listOf(1L, 2L, 3L), sorted.map(TimelineEvent::timelineEventId))
    }

    @Test
    fun `시각이 같으면 id 로 가른다`() {
        // 생성은 겹침 보정이 없어 같은 시각이 실제로 생긴다. 2차 키가 없으면 볼 때마다 순서가 바뀐다.
        val sorted = listOf(event(9, 10, 0), event(4, 10, 0), event(7, 10, 0)).sortedForDisplay()

        assertEquals(listOf(4L, 7L, 9L), sorted.map(TimelineEvent::timelineEventId))
    }

    @Test
    fun `이미 정렬된 목록은 그대로 둔다`() {
        val events = listOf(event(1, 8, 0), event(2, 9, 0))

        assertEquals(events, events.sortedForDisplay())
    }

    private fun event(
        id: Long,
        hour: Int,
        minute: Int,
    ) = TimelineEvent(
        timelineEventId = id,
        eventType = TimelineEventType.UNKNOWN,
        startAt = LocalDateTime.of(2026, 5, 8, hour, minute),
        endAt = null,
        title = "이벤트 $id",
        subtitle = null,
        memo = null,
        question = null,
        items = emptyList(),
    )
}
