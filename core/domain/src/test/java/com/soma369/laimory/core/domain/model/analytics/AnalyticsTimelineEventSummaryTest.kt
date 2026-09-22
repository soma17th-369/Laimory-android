package com.soma369.laimory.core.domain.model.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsTimelineEventSummaryTest {
    @Test
    fun `질문이 있으면 AI, 없으면 직접 추가로 나눠 메모를 센다`() {
        val summary =
            AnalyticsTimelineEventSummary.of(
                events =
                    listOf(
                        event(1L, question = "누구와 있었나요?", memo = "친구랑"),
                        event(2L, question = "무엇을 먹었나요?", memo = null),
                        event(3L, question = null, memo = "직접 쓴 일정"),
                        event(4L, question = null, memo = null),
                    ),
                editLog = AnalyticsTimelineEditLog.EMPTY,
            )

        assertEquals(2, summary.aiEventCount)
        assertEquals(1, summary.aiMemoEventCount)
        assertEquals(2, summary.manualEventCount)
        assertEquals(1, summary.manualMemoEventCount)
    }

    @Test
    fun `공백뿐인 메모와 질문은 없는 것으로 본다`() {
        val summary =
            AnalyticsTimelineEventSummary.of(
                events = listOf(event(1L, question = "  ", memo = " \n")),
                editLog = AnalyticsTimelineEditLog.EMPTY,
            )

        assertEquals(0, summary.aiEventCount)
        assertEquals(1, summary.manualEventCount)
        assertEquals(0, summary.manualMemoEventCount)
    }

    @Test
    fun `고친 이벤트는 완료 순간에 남아 있는 AI 이벤트만 센다`() {
        val summary =
            AnalyticsTimelineEventSummary.of(
                events =
                    listOf(
                        event(1L, question = "누구와 있었나요?", memo = null),
                        event(3L, question = null, memo = null),
                    ),
                // 2 는 고친 뒤 지웠고, 3 은 직접 추가한 이벤트다.
                editLog = AnalyticsTimelineEditLog(editedEventIds = setOf(1L, 2L, 3L), deletedAiEventIds = setOf(2L, 9L)),
            )

        assertEquals(1, summary.aiEditedEventCount)
        assertEquals(2, summary.aiDeletedEventCount)
    }

    private fun event(
        id: Long,
        question: String?,
        memo: String?,
    ) = AnalyticsTimelineEventSnapshot(timelineEventId = id, question = question, memo = memo)
}
