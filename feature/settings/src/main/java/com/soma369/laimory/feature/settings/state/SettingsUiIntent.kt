package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface SettingsUiIntent : UiIntent {
    data object LogoutClicked : SettingsUiIntent

    data object LogoutDismissed : SettingsUiIntent

    data object LogoutConfirmed : SettingsUiIntent
}
