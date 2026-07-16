package com.soma369.laimory.feature.login.state

import com.soma369.laimory.core.domain.model.auth.SocialLoginCallback
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.ui.base.UiIntent

sealed interface LoginUiIntent : UiIntent {
    data class ProviderClicked(
        val provider: SocialLoginProvider,
    ) : LoginUiIntent

    data class CallbackReceived(
        val callback: SocialLoginCallback,
    ) : LoginUiIntent {
        override fun toString(): String = "CallbackReceived(REDACTED)"
    }

    data object BrowserReturnedWithoutCallback : LoginUiIntent

    data object AuthorizationLaunchFailed : LoginUiIntent
}
