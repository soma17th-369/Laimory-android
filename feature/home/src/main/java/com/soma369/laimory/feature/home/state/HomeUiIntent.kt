package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface HomeUiIntent : UiIntent {
    data object LoadIntroInfo : HomeUiIntent

    data object NavigateToCollection : HomeUiIntent
}
