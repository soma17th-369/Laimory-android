package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TimelineRecordSessionRepositoryImplTest {
    private val repository = TimelineRecordSessionRepositoryImpl()

    @Test
    fun `save - 현재 타임라인을 노출한다`() {
        val timeline = timeline(events = listOf(event(id = 1L)))

        repository.save(timeline)

        assertEquals(timeline, repository.timeline.value)
    }

    @Test
    fun `replaceEvent - 동일 ID Event만 같은 위치에서 교체한다`() {
        val first = event(id = 1L, title = "기존 1")
        val second = event(id = 2L, title = "기존 2")
        repository.save(timeline(events = listOf(first, second)))

        repository.replaceEvent(first.copy(title = "수정됨"))

        assertEquals(listOf("수정됨", "기존 2"), repository.timeline.value?.events?.map { it.title })
    }

    @Test
    fun `replaceEvent - 없는 ID는 기존 상태를 유지한다`() {
        val timeline = timeline(events = listOf(event(id = 1L)))
        repository.save(timeline)

        repository.replaceEvent(event(id = 9L))

        assertEquals(timeline, repository.timeline.value)
    }

    @Test
    fun `removeEvent - 마지막 Event를 지워도 빈 DailyTimeline은 유지한다`() {
        repository.save(timeline(events = listOf(event(id = 1L))))

        repository.removeEvent(1L)

        assertEquals(emptyList<TimelineEvent>(), repository.timeline.value?.events)
    }

    @Test
    fun `clear - 현재 타임라인을 제거한다`() {
        repository.save(timeline(events = listOf(event(id = 1L))))

        repository.clear()

        assertNull(repository.timeline.value)
    }

    private fun timeline(events: List<TimelineEvent>) =
        DailyTimeline(
            dailyRecordId = 31L,
            recordDate = LocalDate.of(2026, 5, 8),
            emotion = null,
            events = events,
        )

    private fun event(
        id: Long,
        title: String = "이벤트",
    ) = TimelineEvent(
        timelineEventId = id,
        eventType = TimelineEventType.WORK,
        startAt = LocalDateTime.of(2026, 5, 8, 9, 0),
        endAt = null,
        title = title,
        subtitle = null,
        memo = null,
        items = emptyList(),
    )
}
