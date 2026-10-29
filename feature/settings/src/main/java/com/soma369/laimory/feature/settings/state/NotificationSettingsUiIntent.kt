package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalTime

sealed interface NotificationSettingsUiIntent : UiIntent {
    /** 화면 진입. 서버 값을 조회한다. */
    data object Initialize : NotificationSettingsUiIntent

    data object RetryLoad : NotificationSettingsUiIntent

    data object NavigateBack : NotificationSettingsUiIntent

    /** 전체 알림. 서버로 보낸다. */
    data class PushToggled(
        val isEnabled: Boolean,
    ) : NotificationSettingsUiIntent

    /** 일일 리마인더. 기기에 바로 저장한다. */
    data class DailyReminderToggled(
        val isEnabled: Boolean,
    ) : NotificationSettingsUiIntent

    data object OpenReminderTimePicker : NotificationSettingsUiIntent

    /** 시트 안에서 굴리는 값. 확인 전에는 저장하지 않는다. */
    data class ReminderTimeChanged(
        val time: LocalTime,
    ) : NotificationSettingsUiIntent

    data object ConfirmReminderTime : NotificationSettingsUiIntent

    data object DismissReminderTimePicker : NotificationSettingsUiIntent
}
