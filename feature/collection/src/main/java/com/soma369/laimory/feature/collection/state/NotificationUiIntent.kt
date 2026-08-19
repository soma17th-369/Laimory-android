package com.soma369.laimory.feature.collection.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface NotificationUiIntent : UiIntent {
    /** 기본 키워드 사용 여부를 토글한다. */
    data object ToggleDefaultKeywords : NotificationUiIntent

    /** 키워드 필터를 추가한다. */
    data class AddKeyword(val keyword: String) : NotificationUiIntent

    /** 키워드 필터를 제거한다. */
    data class RemoveKeyword(val keyword: String) : NotificationUiIntent

    /** 앱 allowlist 에서 해당 패키지의 포함 여부를 토글한다. */
    data class ToggleApp(val packageName: String) : NotificationUiIntent

    /** 스테이징된 알림을 모두 비운다(일괄 삭제). */
    data object ClearStaged : NotificationUiIntent
}
