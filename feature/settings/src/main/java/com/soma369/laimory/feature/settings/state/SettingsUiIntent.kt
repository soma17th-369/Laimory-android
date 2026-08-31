package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface SettingsUiIntent : UiIntent {
    /** 화면 진입·복귀. 아직 못 받은 닉네임을 다시 요청한다. */
    data object RefreshProfile : SettingsUiIntent

    /**
     * 아직 못 받은 약관 주소를 다시 요청한다.
     *
     * 화면 진입·복귀와 항목을 눌렀을 때 모두 같은 곳으로 모은다 — 주소가 없으면 그 줄은 눌러도
     * 열 곳이 없으므로, 대신 다시 물어보는 것이 사용자가 할 수 있는 유일한 일이다.
     */
    data object RefreshTermLinks : SettingsUiIntent

    data object LogoutClicked : SettingsUiIntent

    data object LogoutDismissed : SettingsUiIntent

    data object LogoutConfirmed : SettingsUiIntent

    data object AccountDeleteClicked : SettingsUiIntent

    data object AccountDeleteDismissed : SettingsUiIntent

    /** 확인 체크박스를 켠 뒤 삭제를 눌렀다. Dialog 가 체크 전 확인을 막으므로 동의는 이미 받았다. */
    data object AccountDeleteConfirmed : SettingsUiIntent
}
