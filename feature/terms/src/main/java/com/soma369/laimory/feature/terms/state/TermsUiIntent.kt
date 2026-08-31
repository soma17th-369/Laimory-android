package com.soma369.laimory.feature.terms.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface TermsUiIntent : UiIntent {
    data class AgeConfirmationChanged(val isConfirmed: Boolean) : TermsUiIntent

    data object AgreeClicked : TermsUiIntent

    /** 판정 조회가 실패했을 때 다시 묻는다. */
    data object RetryClicked : TermsUiIntent

    /** 다른 계정으로 들어갈 수 있게 남겨 두는 길. 약관 화면에 갇히지 않게 한다. */
    data object LogoutClicked : TermsUiIntent
}
