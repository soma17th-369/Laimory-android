package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface TimelineRecordUiIntent : UiIntent {
    data object NavigateBack : TimelineRecordUiIntent

    data object RequestDelete : TimelineRecordUiIntent

    data object ConfirmDelete : TimelineRecordUiIntent

    data object DismissDelete : TimelineRecordUiIntent

    data object FinishDelete : TimelineRecordUiIntent

    data class SelectEvent(
        val timelineEventId: Long,
    ) : TimelineRecordUiIntent
}
