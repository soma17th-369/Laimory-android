package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface SettingsUiIntent : UiIntent {
    /** 화면 진입·복귀. 아직 못 받은 닉네임을 다시 요청한다. */
    data object RefreshProfile : SettingsUiIntent

    data object LogoutClicked : SettingsUiIntent

    data object LogoutDismissed : SettingsUiIntent

    data object LogoutConfirmed : SettingsUiIntent
}
