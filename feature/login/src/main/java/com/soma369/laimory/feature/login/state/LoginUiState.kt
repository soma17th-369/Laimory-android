package com.soma369.laimory.feature.login.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class LoginUiState(
    val phase: LoginPhase = LoginPhase.IDLE,
    val activeProvider: SocialLoginProvider? = null,
    val errorMessage: String? = null,
) : UiState {
    val isInteractionDisabled: Boolean get() = phase != LoginPhase.IDLE
}

enum class LoginPhase {
    IDLE,
    PREPARING,
    WAITING_CALLBACK,
    EXCHANGING_TOKEN,
}
