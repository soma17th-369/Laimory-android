package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface TimelineUiSideEffect : UiSideEffect {
    data class ShowSnackbar(val message: String) : TimelineUiSideEffect
}
