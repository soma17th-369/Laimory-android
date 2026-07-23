package com.soma369.laimory.core.domain.model.timeline

/** 한 초안 작업을 관찰하면서 발생하는 polling 이벤트. */
sealed interface DraftTaskPollingEvent {
    data class Status(
        val outcome: DraftTaskStatusOutcome,
    ) : DraftTaskPollingEvent

    data class LongRunning(
        val elapsedSeconds: Long,
    ) : DraftTaskPollingEvent

    data class RetryableFailure(
        val cause: Throwable,
    ) : DraftTaskPollingEvent
}
