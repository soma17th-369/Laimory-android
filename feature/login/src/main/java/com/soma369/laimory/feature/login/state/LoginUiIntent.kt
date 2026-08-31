package com.soma369.laimory.feature.login.state

import com.soma369.laimory.core.domain.model.auth.SocialLoginCallback
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.ui.base.UiIntent

sealed interface LoginUiIntent : UiIntent {
    /**
     * 아직 못 받은 약관 주소를 다시 요청한다.
     *
     * ViewModel 이 Activity 수명이라 한 번만 부르면 첫 조회가 실패한 세션 내내 눌리지 않는
     * 문구로 남는다. 로그인 전에도 볼 수 있어야 하는 문서라 화면이 뜰 때마다 다시 묻는다.
     */
    data object RefreshTermLinks : LoginUiIntent

    data class ProviderClicked(
        val provider: SocialLoginProvider,
    ) : LoginUiIntent

    data class CallbackReceived(
        val callback: SocialLoginCallback,
    ) : LoginUiIntent {
        override fun toString(): String = "CallbackReceived(REDACTED)"
    }

    data object BrowserReturnedWithoutCallback : LoginUiIntent

    data object AuthorizationLaunchFailed : LoginUiIntent
}
