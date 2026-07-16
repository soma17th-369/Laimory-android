package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class SettingsUiState(
    val accountProvider: SocialLoginProvider? = null,
    val isLogoutDialogVisible: Boolean = false,
    val isLoggingOut: Boolean = false,
) : UiState
