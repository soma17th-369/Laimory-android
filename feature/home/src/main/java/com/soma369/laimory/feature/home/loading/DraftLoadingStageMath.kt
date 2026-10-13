package com.soma369.laimory.feature.home.loading

import kotlin.time.Duration

/**
 * 진행 카드 각 줄의 상태를 경과 시간으로 계산한다.
 *
 * 앞의 세 줄은 서버의 실제 진척률이 아니라 대기 연출이다. 서버는 `PROCESSING`/`SUCCESS`/`FAILED`만
 * 알려주므로 중간 진척을 만들어낼 수 없고, 만들어낸 숫자를 사실처럼 보이게 하지 않는다. 줄마다 언제 끝날지는
 * [DraftLoadingStagePlan] 이 고른 개수로 정한다.
 *
 * 서버 완료가 확인되면([isCompleted]) 남은 연출을 건너뛰고 모두 완료로 본다 — 사용자를 장식 때문에
 * 더 기다리게 하지 않는다.
 */
internal object DraftLoadingStageMath {
    fun stateOf(
        stage: DraftLoadingStage,
        elapsed: Duration,
        isCompleted: Boolean,
        plan: DraftLoadingStagePlan,
    ): DraftLoadingStageState {
        if (isCompleted) return DraftLoadingStageState.DONE
        val deadline = plan.deadlines[stage] ?: return aiStateOf(elapsed, plan)
        if (elapsed >= deadline) return DraftLoadingStageState.DONE
        val start = plan.deadlines.values.filter { it < deadline }.maxOrNull() ?: Duration.ZERO
        return if (elapsed >= start) DraftLoadingStageState.IN_PROGRESS else DraftLoadingStageState.PENDING
    }

    /** AI 줄은 연출이 아니라 실제 완료로만 끝난다. 앞 줄이 다 끝난 뒤부터 진행 중으로 보인다. */
    private fun aiStateOf(
        elapsed: Duration,
        plan: DraftLoadingStagePlan,
    ): DraftLoadingStageState = if (elapsed >= plan.lastDeadline) DraftLoadingStageState.IN_PROGRESS else DraftLoadingStageState.PENDING
}
