package com.soma369.laimory.analytics

import com.soma369.laimory.core.domain.model.analytics.AnalyticsCreateResult
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsFailureCode
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.DraftTaskFailureReason
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.model.timeline.DraftTaskUnavailableReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DraftTaskResultReporterTest {
    private val task = ActiveDraftTask("task-1", LocalDate.parse("2026-09-21"), Instant.parse("2026-09-21T01:00:00Z"))

    @Test
    fun `성공은 실패 코드 없이 작업 ID 와 함께 낸다`() {
        assertEquals(
            "task-1" to AnalyticsEvent.TimelineCreateResult(AnalyticsCreateResult.SUCCESS, failureCode = null),
            DraftTaskTrackingState.Success(task).toCreateResult(),
        )
    }

    @Test
    fun `입력 부족 실패는 따로 가른다`() {
        val (_, event) = DraftTaskTrackingState.Failed(task, DraftTaskFailureReason.STAGING_DATA_MISSING).toCreateResult()!!

        assertEquals(AnalyticsFailureCode.INSUFFICIENT_EVENT, event.failureCode)
    }

    @Test
    fun `결과를 받을 수 없으면 결과 없음 실패다`() {
        val (_, event) = DraftTaskTrackingState.Unavailable(task, DraftTaskUnavailableReason.RESULT).toCreateResult()!!

        assertEquals(AnalyticsCreateResult.FAILURE, event.result)
        assertEquals(AnalyticsFailureCode.RESULT_UNAVAILABLE, event.failureCode)
    }

    @Test
    fun `진행 중이거나 재시도 가능한 오류는 끝이 아니다`() {
        assertNull(DraftTaskTrackingState.Processing(task).toCreateResult())
        assertNull(DraftTaskTrackingState.LongRunning(task, elapsedSeconds = 90).toCreateResult())
        assertNull(DraftTaskTrackingState.RetryableError(task).toCreateResult())
        assertNull(DraftTaskTrackingState.Idle.toCreateResult())
    }
}
