package com.soma369.laimory.feature.home.loading

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DraftLoadingStageMathTest {
    private fun stateOf(
        stage: DraftLoadingStage,
        elapsed: Duration,
        isCompleted: Boolean = false,
    ) = DraftLoadingStageMath.stateOf(stage, elapsed, isCompleted)

    @Test
    fun `진입 직후에는 사진만 진행 중이고 나머지는 대기다`() {
        assertEquals(DraftLoadingStageState.IN_PROGRESS, stateOf(DraftLoadingStage.PHOTO, Duration.ZERO))
        assertEquals(DraftLoadingStageState.PENDING, stateOf(DraftLoadingStage.CALENDAR, Duration.ZERO))
        assertEquals(DraftLoadingStageState.PENDING, stateOf(DraftLoadingStage.STAY, Duration.ZERO))
        assertEquals(DraftLoadingStageState.PENDING, stateOf(DraftLoadingStage.AI, Duration.ZERO))
    }

    @Test
    fun `앞 단계는 정해진 시각에 차례로 끝난다`() {
        assertEquals(DraftLoadingStageState.DONE, stateOf(DraftLoadingStage.PHOTO, 8.seconds))
        assertEquals(DraftLoadingStageState.IN_PROGRESS, stateOf(DraftLoadingStage.CALENDAR, 8.seconds))

        assertEquals(DraftLoadingStageState.DONE, stateOf(DraftLoadingStage.CALENDAR, 16.seconds))
        assertEquals(DraftLoadingStageState.IN_PROGRESS, stateOf(DraftLoadingStage.STAY, 16.seconds))

        assertEquals(DraftLoadingStageState.DONE, stateOf(DraftLoadingStage.STAY, 24.seconds))
    }

    @Test
    fun `AI 단계는 앞 단계가 끝난 뒤에 진행 중이 되고 시간만으로는 끝나지 않는다`() {
        assertEquals(DraftLoadingStageState.PENDING, stateOf(DraftLoadingStage.AI, 23.seconds))
        assertEquals(DraftLoadingStageState.IN_PROGRESS, stateOf(DraftLoadingStage.AI, 24.seconds))
        // 아무리 기다려도 서버 완료 없이는 끝나지 않는다.
        assertEquals(DraftLoadingStageState.IN_PROGRESS, stateOf(DraftLoadingStage.AI, 10.seconds * 60))
    }

    @Test
    fun `서버 완료가 먼저 확인되면 남은 연출을 건너뛴다`() {
        DraftLoadingStage.entries.forEach { stage ->
            assertEquals(
                DraftLoadingStageState.DONE,
                stateOf(stage, elapsed = 1.seconds, isCompleted = true),
            )
        }
    }
}
