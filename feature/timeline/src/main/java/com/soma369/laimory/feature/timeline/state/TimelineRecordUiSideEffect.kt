package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface TimelineRecordUiSideEffect : UiSideEffect {
    data object OpenRecordMenu : TimelineRecordUiSideEffect

    data class NavigateToEventEditor(
        val timelineEventId: Long,
    ) : TimelineRecordUiSideEffect
}
