package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.model.terms.TermLinks
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class SettingsUiState(
    val accountProvider: SocialLoginProvider? = null,
    /** 계정 카드 제목에 쓸 닉네임. 없으면 로그인 제공자 문구로 대체한다. */
    val nickname: String? = null,
    val isLoggingOut: Boolean = false,
    val isWithdrawing: Boolean = false,
    /** 약관 원문 주소. 조회 전이거나 실패하면 비어 있고, 그때 정보 항목은 눌리지 않는다. */
    val termLinks: TermLinks = TermLinks(),
    /** 위치 자동 수집을 사용자가 켜 둔 상태인지. 끈 적이 없으면 참이므로 기본값도 참이다. */
    val isLocationCollectionEnabled: Boolean = true,
    /** `공지사항` 줄에 새 공지 표시를 띄울지. 모르면 띄우지 않는다. */
    val hasNewNotice: Boolean = false,
    /** 계정 카드 오른쪽의 남은 크레딧. 받기 전이거나 못 받으면 `null` 이고 칸을 비운다(0 으로 보이면 안 된다). */
    val remainingCredits: Int? = null,
    /**
     * 남은 크레딧을 받는 중. 조회가 늦을 수 있어 값이 없는 동안 숫자 자리에 로딩을 둔다 — 이미 받은 값이 있으면 화면 복귀마다
     * 다시 받는 동안에도 그 숫자를 둔다(로딩으로 바꾸면 깜빡인다). 처음엔 참이다 — 화면이 뜬 뒤 조회를 보내기 전 한순간
     * `-`(못 받음)가 비쳤다 로딩으로 바뀌지 않게.
     */
    val isCreditsLoading: Boolean = true,
    /**
     * `사용 방법` 시트. 떠 있는 동안 `타임라인 편집 둘러보기` 가 열 기록을 함께 든다. null 이면 닫혀 있다.
     */
    val usageGuide: UsageGuideTimelineTarget? = null,
) : UiState {
    /** 계정 관련 동작 하나가 진행 중이면 나머지 항목도 잠근다. */
    val isAccountActionInProgress: Boolean get() = isLoggingOut || isWithdrawing

    /** 두 주소를 다 받았는지. 하나라도 없으면 다시 묻는다. */
    val hasTermLinks: Boolean
        get() = termLinks.termsOfService != null && termLinks.privacyPolicy != null
}
