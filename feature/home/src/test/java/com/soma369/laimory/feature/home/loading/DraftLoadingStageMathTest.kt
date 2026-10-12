package com.soma369.laimory.feature.home.loading

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DraftLoadingStageMathTest {
    /** 계산 규칙만 본다. 길이는 [DraftLoadingStagePlanTest] 가 따로 본다. */
    private val plan =
        DraftLoadingStagePlan(
            deadlines =
                mapOf(
                    DraftLoadingStage.PHOTO to 10.seconds,
                    DraftLoadingStage.CALENDAR to 14.seconds,
                    DraftLoadingStage.STAY to 20.seconds,
                ),
        )

    private fun stateOf(
        stage: DraftLoadingStage,
        elapsed: Duration,
        isCompleted: Boolean = false,
    ) = DraftLoadingStageMath.stateOf(stage, elapsed, isCompleted, plan)

    @Test
    fun `진입 직후에는 사진만 진행 중이고 나머지는 대기다`() {
        assertEquals(DraftLoadingStageState.IN_PROGRESS, stateOf(DraftLoadingStage.PHOTO, Duration.ZERO))
        assertEquals(DraftLoadingStageState.PENDING, stateOf(DraftLoadingStage.CALENDAR, Duration.ZERO))
        assertEquals(DraftLoadingStageState.PENDING, stateOf(DraftLoadingStage.STAY, Duration.ZERO))
        assertEquals(DraftLoadingStageState.PENDING, stateOf(DraftLoadingStage.AI, Duration.ZERO))
    }

    @Test
    fun `앞 단계는 계획한 시각에 차례로 끝난다`() {
        assertEquals(DraftLoadingStageState.DONE, stateOf(DraftLoadingStage.PHOTO, 10.seconds))
        assertEquals(DraftLoadingStageState.IN_PROGRESS, stateOf(DraftLoadingStage.CALENDAR, 10.seconds))

        assertEquals(DraftLoadingStageState.DONE, stateOf(DraftLoadingStage.CALENDAR, 14.seconds))
        assertEquals(DraftLoadingStageState.IN_PROGRESS, stateOf(DraftLoadingStage.STAY, 14.seconds))

        assertEquals(DraftLoadingStageState.DONE, stateOf(DraftLoadingStage.STAY, 20.seconds))
    }

    @Test
    fun `AI 단계는 앞 단계가 끝난 뒤에 진행 중이 되고 시간만으로는 끝나지 않는다`() {
        assertEquals(DraftLoadingStageState.PENDING, stateOf(DraftLoadingStage.AI, 19.seconds))
        assertEquals(DraftLoadingStageState.IN_PROGRESS, stateOf(DraftLoadingStage.AI, 20.seconds))
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
