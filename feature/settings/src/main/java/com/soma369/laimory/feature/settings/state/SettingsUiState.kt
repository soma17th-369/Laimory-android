package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class SettingsUiState(
    val accountProvider: SocialLoginProvider? = null,
    /** 계정 카드 제목에 쓸 닉네임. 없으면 로그인 제공자 문구로 대체한다. */
    val nickname: String? = null,
    val isLoggingOut: Boolean = false,
    val isWithdrawing: Boolean = false,
) : UiState {
    /** 계정 관련 동작 하나가 진행 중이면 나머지 항목도 잠근다. */
    val isAccountActionInProgress: Boolean get() = isLoggingOut || isWithdrawing
}
