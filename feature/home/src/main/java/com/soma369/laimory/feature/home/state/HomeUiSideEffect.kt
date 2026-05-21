package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface HomeUiSideEffect : UiSideEffect {
    data class ShowToast(val message: String) : HomeUiSideEffect
}
