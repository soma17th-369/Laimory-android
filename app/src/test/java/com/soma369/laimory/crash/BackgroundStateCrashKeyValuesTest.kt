package com.soma369.laimory.crash

import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BackgroundStateCrashKeyValuesTest {
    @Test
    fun `켜 뒀어도 권한이 없으면 no_permission 이다`() {
        // 부팅 때 복원된 수집이 위치를 받지 못하던 경우다. 의사만 보면 on 으로 보여 리포트로 가려지지 않는다.
        assertEquals("no_permission", locationTrackingKeyValue(enabled = true, canTrack = false))
    }

    @Test
    fun `사용자가 끄면 권한과 무관하게 off 다`() {
        assertEquals("off", locationTrackingKeyValue(enabled = false, canTrack = true))
        assertEquals("off", locationTrackingKeyValue(enabled = false, canTrack = false))
    }

    @Test
    fun `켜 두고 권한도 있으면 on 이다`() {
        assertEquals("on", locationTrackingKeyValue(enabled = true, canTrack = true))
    }

    @Test
    fun `초안 작업은 단계만 남긴다`() {
        val task = ActiveDraftTask(taskId = "task-1", recordDate = LocalDate.of(2026, 9, 14), requestedAt = Instant.EPOCH)

        val values =
            listOf(
                DraftTaskTrackingState.Idle,
                DraftTaskTrackingState.Processing(task, elapsedSeconds = 30),
                DraftTaskTrackingState.LongRunning(task, elapsedSeconds = 900),
                DraftTaskTrackingState.Success(task, eventCount = 1),
                DraftTaskTrackingState.RetryableError(task),
            ).map(::draftTaskKeyValue)

        assertEquals(listOf("idle", "processing", "long_running", "success", "retryable_error"), values)
    }
}
