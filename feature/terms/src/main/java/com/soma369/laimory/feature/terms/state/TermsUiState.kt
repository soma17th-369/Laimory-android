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
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) : UiState {
    val isLoading: Boolean get() = gate == TermsGateState.Unknown

    val hasFailed: Boolean get() = gate == TermsGateState.Failed

    /**
     * 만 14세 이상 확인은 여기서 받지 않는다.
     *
     * 온보딩 마지막 장이 필수 확인 목록에서 함께 받고 **기록까지 남긴다** — 두 자리에서 물으면
     * 같은 질문을 두 번 받게 되고, 이 화면의 확인은 어디에도 남지 않아 근거가 되지도 않는다.
     */
    val canAgree: Boolean get() = termsOfService != null && !isSubmitting
}
