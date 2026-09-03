package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState

/**
 * @param updatingToggles 서버 응답을 기다리는 줄. 표시값은 성공해야 바뀌므로, 그동안은 그 줄만
 *   잠가 같은 값을 두 번 보내지 않게 한다.
 */
@Immutable
data class NotificationSettingsUiState(
    val content: NotificationSettingsUiContent = NotificationSettingsUiContent.Loading,
    val updatingToggles: Set<NotificationToggle> = emptySet(),
) : UiState {
    val settings = (content as? NotificationSettingsUiContent.Settings)?.value

    fun isUpdating(toggle: NotificationToggle): Boolean = toggle in updatingToggles
}
