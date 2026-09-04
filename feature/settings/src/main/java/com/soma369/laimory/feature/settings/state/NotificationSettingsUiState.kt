package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.push.PushSettings
import com.soma369.laimory.core.ui.base.UiState

/**
 * @param content 화면에 그리는 값. 누른 즉시 바뀐다.
 * @param confirmedSettings 서버가 확인해 준 마지막 값. 보낼 것이 남았는지 가리고, 서버가 거절하면
 *   되돌아갈 자리다. 조회 전에는 `null`.
 */
@Immutable
data class NotificationSettingsUiState(
    val content: NotificationSettingsUiContent = NotificationSettingsUiContent.Loading,
    val confirmedSettings: PushSettings? = null,
) : UiState {
    val settings = (content as? NotificationSettingsUiContent.Settings)?.value
}
