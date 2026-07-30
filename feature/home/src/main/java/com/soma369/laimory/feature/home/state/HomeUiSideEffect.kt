package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface HomeUiSideEffect : UiSideEffect {
    data class RequestPhotoAccess(
        val force: Boolean = false,
    ) : HomeUiSideEffect

    data class ShowSnackbar(
        val message: String,
    ) : HomeUiSideEffect
}
