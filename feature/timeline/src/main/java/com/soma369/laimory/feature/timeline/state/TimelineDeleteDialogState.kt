package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable

@Immutable
sealed interface TimelineDeleteDialogState {
    data object Hidden : TimelineDeleteDialogState

    data object Confirmation : TimelineDeleteDialogState

    data object Deleting : TimelineDeleteDialogState

    data class RetryableError(
        val message: String,
    ) : TimelineDeleteDialogState

    data object Success : TimelineDeleteDialogState
}
