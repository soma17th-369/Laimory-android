package com.soma369.laimory.feature.collection.state

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface CalendarUiSideEffect : UiSideEffect {
    data class ShowMessage(val message: String) : CalendarUiSideEffect
}
