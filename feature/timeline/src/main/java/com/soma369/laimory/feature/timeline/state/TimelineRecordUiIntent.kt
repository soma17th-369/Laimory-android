package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface TimelineRecordUiIntent : UiIntent {
    data object NavigateBack : TimelineRecordUiIntent

    data object OpenRecordMenu : TimelineRecordUiIntent

    data class SelectEvent(
        val timelineEventId: Long,
    ) : TimelineRecordUiIntent
}
