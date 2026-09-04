package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.push.PushSettings

/**
 * 알림 화면의 본문.
 *
 * 서버가 단일 권위라 기기에 복제해 두지 않는다. 그래서 조회가 실패하면 **보여 줄 값이 없다** —
 * 임의 기본값을 그리면 켜 둔 적 없는 설정을 켜져 있다고 말하게 된다.
 */
@Immutable
sealed interface NotificationSettingsUiContent {
    data object Loading : NotificationSettingsUiContent

    data object LoadFailed : NotificationSettingsUiContent

    data class Settings(
        val value: PushSettings,
    ) : NotificationSettingsUiContent
}
