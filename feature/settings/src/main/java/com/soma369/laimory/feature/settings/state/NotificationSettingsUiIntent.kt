package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface NotificationSettingsUiIntent : UiIntent {
    /** 화면 진입. 서버 값을 조회한다. */
    data object Initialize : NotificationSettingsUiIntent

    data object RetryLoad : NotificationSettingsUiIntent

    data object NavigateBack : NotificationSettingsUiIntent

    data class ToggleChanged(
        val toggle: NotificationToggle,
        val isEnabled: Boolean,
    ) : NotificationSettingsUiIntent
}
