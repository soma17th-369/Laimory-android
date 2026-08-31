package com.soma369.laimory.feature.terms.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class TermsUiState(
    val gate: TermsGateState = TermsGateState.Unknown,
    /** 동의받아야 하는 이용약관. 판정이 서기 전이거나 통과 상태면 `null` 이다. */
    val termsOfService: TermDocument? = null,
    /** 동의 대상이 아니라 함께 안내하는 처리방침. 없으면 링크가 눌리지 않는다. */
    val privacyPolicy: TermDocument? = null,
    /**
     * 만 14세 이상 자기확인.
     *
     * 약관 동의와 섞지 않는다 — 가입 자격 확인이지 동의가 아니고, 서버에도 보내지 않는다.
     * 기본은 해제다.
     */
    val isAgeConfirmed: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) : UiState {
    val isLoading: Boolean get() = gate == TermsGateState.Unknown

    val hasFailed: Boolean get() = gate == TermsGateState.Failed

    val canAgree: Boolean get() = termsOfService != null && isAgeConfirmed && !isSubmitting
}
