package com.soma369.laimory.feature.login.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.model.terms.TermLinks
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class LoginUiState(
    val phase: LoginPhase = LoginPhase.IDLE,
    val activeProvider: SocialLoginProvider? = null,
    val errorMessage: String? = null,
    /** 약관 원문 주소. 조회가 늦거나 실패하면 비어 있고, 그때는 링크만 눌리지 않는다. */
    val termLinks: TermLinks = TermLinks(),
) : UiState {
    val isInteractionDisabled: Boolean get() = phase != LoginPhase.IDLE
}
