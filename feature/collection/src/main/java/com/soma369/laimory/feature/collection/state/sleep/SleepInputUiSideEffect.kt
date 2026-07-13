package com.soma369.laimory.feature.collection.state.sleep

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface SleepInputUiSideEffect : UiSideEffect {
    data class ShowMessage(val message: String) : SleepInputUiSideEffect
}
