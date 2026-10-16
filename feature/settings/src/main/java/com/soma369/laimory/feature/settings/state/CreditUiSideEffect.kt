package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface CreditUiSideEffect : UiSideEffect {
    data class ShowSnackbar(
        val message: String,
    ) : CreditUiSideEffect
}
