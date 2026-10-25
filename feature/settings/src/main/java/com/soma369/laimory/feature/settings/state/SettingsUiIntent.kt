package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiIntent
import com.soma369.laimory.core.ui.permission.DataPermissionEvent

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

    /** `앱 설정 > 알림`. 값을 이 화면에서 바꾸지 않고 알림 화면으로 넘어간다. */
    data object NotificationSettingsClicked : SettingsUiIntent

    /** `앱 설정 > 테마`. */
    data object ThemeSettingsClicked : SettingsUiIntent

    /** `지원 > 공지사항`. */
    data object NoticesClicked : SettingsUiIntent

    /** `지원 > 문의하기`. */
    data object InquiryClicked : SettingsUiIntent

    /** `지원 > 사용 방법`. 다시 볼 튜토리얼을 고르는 시트를 연다. */
    data object UsageGuideClicked : SettingsUiIntent

    data object UsageGuideDismissed : SettingsUiIntent

    /** `처음부터 따라 하기`. 봤다는 기록을 지우고 홈으로 간다 — 추천 날짜 시트부터 다시 뜬다. */
    data object ReplayFullTutorial : SettingsUiIntent

    /** `홈 화면 둘러보기`. 홈으로 가서 지금 날짜 그대로 코치마크만 짚는다. */
    data object ReplayHomeTutorial : SettingsUiIntent

    /** `타임라인 편집 둘러보기`. 이벤트가 있는 가장 최근 기록을 열어 짚는다. 받지 못했으면 다시 받는다. */
    data object ReplayTimelineTutorial : SettingsUiIntent

    /**
     * 화면 진입·복귀. 새 공지 표시를 다시 판정한다.
     *
     * 공지 목록에서 읽고 돌아오면 사라지고, 기간이 지나도 사라져야 하므로 한 번 정해 두지 않는다.
     */
    data object RefreshNoticeBadge : SettingsUiIntent

    /** 계정 카드의 `남은 크레딧` 칸. 크레딧 화면으로 넘어간다. */
    data object CreditsClicked : SettingsUiIntent

    /** 화면 진입·복귀. 남은 크레딧은 만들기로 줄어드는 값이라 들어올 때마다 다시 받는다. */
    data object RefreshCredits : SettingsUiIntent

    /**
     * 위치 자동 수집을 켜거나 끈다.
     *
     * 권한과 나눠 둔다 — 권한은 시스템이 갖고 이 값은 앱이 갖는다. 여기서 끈 것은 앱이 다시
     * 켜지 않는다(전경 진입의 상태 맞추기가 사용자의 의사를 넘지 않는다).
     */
    data class LocationCollectionToggled(
        val enabled: Boolean,
    ) : SettingsUiIntent

    data object LogoutClicked : SettingsUiIntent

    data object LogoutDismissed : SettingsUiIntent

    data object LogoutConfirmed : SettingsUiIntent

    data object AccountDeleteClicked : SettingsUiIntent

    data object AccountDeleteDismissed : SettingsUiIntent

    /** 확인 체크박스를 켠 뒤 삭제를 눌렀다. Dialog 가 체크 전 확인을 막으므로 동의는 이미 받았다. */
    data object AccountDeleteConfirmed : SettingsUiIntent

    /** 이 화면에서 연 권한 요청과 그 결과. 분석에 기록한다. */
    data class PermissionEvent(
        val event: DataPermissionEvent,
    ) : SettingsUiIntent
}
