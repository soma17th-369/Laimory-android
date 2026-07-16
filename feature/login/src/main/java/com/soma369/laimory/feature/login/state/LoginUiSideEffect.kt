package com.soma369.laimory.feature.login.state

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface LoginUiSideEffect : UiSideEffect {
    data class OpenAuthorizationUrl(
        val url: String,
    ) : LoginUiSideEffect
}
