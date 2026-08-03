package com.soma369.laimory.core.domain.model.timeline

/** 앱 전역에서 공유하는 현재 초안 작업 상태. */
sealed interface DraftTaskTrackingState {
    data object Idle : DraftTaskTrackingState

    sealed interface WithTask : DraftTaskTrackingState {
        val task: ActiveDraftTask
    }

    data class Processing(
        override val task: ActiveDraftTask,
        val elapsedSeconds: Long? = null,
    ) : WithTask

    data class LongRunning(
        override val task: ActiveDraftTask,
        val elapsedSeconds: Long,
    ) : WithTask

    data class Success(
        override val task: ActiveDraftTask,
    ) : WithTask

    data class Failed(
        override val task: ActiveDraftTask,
        val reason: DraftTaskFailureReason,
    ) : WithTask

    data class RetryableError(
        override val task: ActiveDraftTask,
    ) : WithTask

    data class Unavailable(
        override val task: ActiveDraftTask,
        val reason: DraftTaskUnavailableReason,
    ) : WithTask
}

enum class DraftTaskUnavailableReason {
    TASK,
    RESULT,
}
