package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface HomeUiIntent : UiIntent {
    data object Increment : HomeUiIntent

    data object Decrement : HomeUiIntent

    data object ShowToast : HomeUiIntent

    data object LoadIntroInfo : HomeUiIntent
}
