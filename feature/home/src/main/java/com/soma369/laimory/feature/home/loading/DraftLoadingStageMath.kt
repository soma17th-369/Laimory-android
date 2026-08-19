package com.soma369.laimory.feature.home.loading

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 진행 카드 각 줄의 상태를 경과 시간으로 계산한다.
 *
 * 앞의 세 줄은 서버의 실제 진척률이 아니라 대기 연출이다. 서버는 `PROCESSING`/`SUCCESS`/`FAILED`만
 * 알려주므로 중간 진척을 만들어낼 수 없고, 만들어낸 숫자를 사실처럼 보이게 하지 않는다.
 *
 * 서버 완료가 확인되면([isCompleted]) 남은 연출을 건너뛰고 모두 완료로 본다 — 사용자를 장식 때문에
 * 더 기다리게 하지 않는다.
 */
internal object DraftLoadingStageMath {
    /** 앞 세 줄이 차례로 끝나는 시각. 마지막 값 이후로는 AI 줄만 진행 중으로 남는다. */
    private val STAGE_DEADLINES =
        mapOf(
            DraftLoadingStage.PHOTO to 8.seconds,
            DraftLoadingStage.CALENDAR to 16.seconds,
            DraftLoadingStage.STAY to 24.seconds,
        )

    fun stateOf(
        stage: DraftLoadingStage,
        elapsed: Duration,
        isCompleted: Boolean,
    ): DraftLoadingStageState {
        if (isCompleted) return DraftLoadingStageState.DONE
        val deadline = STAGE_DEADLINES[stage] ?: return aiStateOf(elapsed)
        if (elapsed >= deadline) return DraftLoadingStageState.DONE
        val start = STAGE_DEADLINES.entries.lastOrNull { it.value < deadline }?.value ?: Duration.ZERO
        return if (elapsed >= start) DraftLoadingStageState.IN_PROGRESS else DraftLoadingStageState.PENDING
    }

    /** AI 줄은 연출이 아니라 실제 완료로만 끝난다. 앞 줄이 다 끝난 뒤부터 진행 중으로 보인다. */
    private fun aiStateOf(elapsed: Duration): DraftLoadingStageState {
        val lastDeadline = STAGE_DEADLINES.values.max()
        return if (elapsed >= lastDeadline) DraftLoadingStageState.IN_PROGRESS else DraftLoadingStageState.PENDING
    }
}
