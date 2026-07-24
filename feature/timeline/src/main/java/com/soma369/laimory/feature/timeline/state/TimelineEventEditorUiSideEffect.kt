package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface TimelineEventEditorUiSideEffect : UiSideEffect {
    data object LaunchPhotoPicker : TimelineEventEditorUiSideEffect

    data object FocusTitle : TimelineEventEditorUiSideEffect

    data class ShowSnackbar(
        val message: String,
    ) : TimelineEventEditorUiSideEffect
}
