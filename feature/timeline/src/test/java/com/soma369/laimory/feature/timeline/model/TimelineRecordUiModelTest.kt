package com.soma369.laimory.feature.timeline.model

import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineItem
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class TimelineRecordUiModelTest {
    @Test
    fun `toUiModel - Item을 타입별 개수로 집계하고 enum 순서로 유지한다`() {
        val startAt = LocalDateTime.of(2026, 5, 8, 9, 0)
        val timeline =
            DailyTimeline(
                dailyRecordId = 31L,
                recordDate = startAt.toLocalDate(),
                emotion = null,
                events =
                    listOf(
                        TimelineEvent(
                            timelineEventId = 1L,
                            eventType = TimelineEventType.PHOTO_MOMENT,
                            startAt = startAt,
                            endAt = null,
                            title = "산책",
                            subtitle = null,
                            memo = null,
                            question = null,
                            items =
                                listOf(
                                    item(1L, TimelineItemType.STAY, startAt),
                                    item(
                                        id = 2L,
                                        type = TimelineItemType.PHOTO,
                                        startAt = startAt,
                                        photoUrl = "https://example.com/photo-2.jpg",
                                    ),
                                    item(3L, TimelineItemType.PHOTO, startAt),
                                ),
                        ),
                    ),
            )

        val event = timeline.toUiModel().events.single()

        assertEquals(
            listOf(
                TimelineItemCountUiModel(TimelineItemType.PHOTO, 2),
                TimelineItemCountUiModel(TimelineItemType.STAY, 1),
            ),
            event.itemCounts,
        )
        assertEquals(listOf("https://example.com/photo-2.jpg", null), event.photoUrls)
    }

    @Test
    fun `toUiModel - question을 UI 모델까지 전달한다`() {
        val startAt = LocalDateTime.of(2026, 5, 8, 9, 0)
        val timeline =
            DailyTimeline(
                dailyRecordId = 31L,
                recordDate = startAt.toLocalDate(),
                emotion = null,
                events =
                    listOf(
                        TimelineEvent(
                            timelineEventId = 1L,
                            eventType = TimelineEventType.MEAL,
                            startAt = startAt,
                            endAt = null,
                            title = "점심",
                            subtitle = null,
                            memo = null,
                            question = "오늘 누구와 함께였나요?",
                            items = emptyList(),
                        ),
                    ),
            )

        assertEquals("오늘 누구와 함께였나요?", timeline.toUiModel().events.single().question)
    }

    private fun item(
        id: Long,
        type: TimelineItemType,
        startAt: LocalDateTime,
        photoUrl: String? = null,
    ) = TimelineItem(
        timelineItemId = id,
        itemType = type,
        rawId = "raw-$id",
        startAt = startAt,
        endAt = null,
        photoUrl = photoUrl,
    )
}
