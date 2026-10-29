package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable

/**
 * 알림 화면의 본문.
 *
 * 전체 알림은 서버가 단일 권위라 기기에 복제해 두지 않는다. 그래서 조회가 실패하면 **보여 줄 값이 없다** —
 * 임의 기본값을 그리면 켜 둔 적 없는 설정을 켜져 있다고 말하게 된다. 일일 리마인더는 기기 값이지만, 전체
 * 알림이 잠금 여부를 정하므로 같은 본문 아래 둔다.
 */
@Immutable
sealed interface NotificationSettingsUiContent {
    data object Loading : NotificationSettingsUiContent

    data object LoadFailed : NotificationSettingsUiContent

    /** @param isPushEnabled 화면에 그리는 전체 알림 값. 누른 즉시 바뀐다. */
    data class Settings(
        val isPushEnabled: Boolean,
    ) : NotificationSettingsUiContent
}
