package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.reminder.DailyReminderSettings
import com.soma369.laimory.core.ui.base.UiState
import java.time.LocalTime

/**
 * @param content 화면에 그리는 전체 알림 값. 누른 즉시 바뀐다.
 * @param confirmedPushEnabled 서버가 확인해 준 마지막 전체 알림 값. 보낼 것이 남았는지 가리고, 서버가 거절하면
 *   되돌아갈 자리다. 조회 전에는 `null`.
 * @param reminder 기기에 저장된 일일 리마인더. 저장소를 그대로 따른다.
 * @param reminderTimeDraft 시각 시트에서 굴리는 값. 시트가 닫혀 있으면 `null`.
 */
@Immutable
data class NotificationSettingsUiState(
    val content: NotificationSettingsUiContent = NotificationSettingsUiContent.Loading,
    val confirmedPushEnabled: Boolean? = null,
    val reminder: DailyReminderSettings = DailyReminderSettings.DEFAULT,
    val reminderTimeDraft: LocalTime? = null,
) : UiState {
    val isPushEnabled: Boolean? = (content as? NotificationSettingsUiContent.Settings)?.isPushEnabled
}
