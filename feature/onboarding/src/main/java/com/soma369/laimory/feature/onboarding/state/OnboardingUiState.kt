package com.soma369.laimory.feature.onboarding.state

import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.ui.base.UiState
import com.soma369.laimory.feature.onboarding.model.ONBOARDING_PAGES
import com.soma369.laimory.feature.onboarding.model.OnboardingPageSpec

data class OnboardingUiState(
    val pages: List<OnboardingPageSpec> = ONBOARDING_PAGES,
    /** 처음 열 장. 중간에 나갔다 오면 마지막으로 본 장이다. `null` 이면 아직 복원 전이다. */
    val initialPageIndex: Int? = null,
    val nickname: String? = null,
    /** 완료 저장 중. 저장이 끝나야 Home 으로 넘어간다. */
    val isCompleting: Boolean = false,
    /** 완료 저장이 실패한 상태. 재시도 수단을 함께 띄운다. */
    val hasCompletionFailed: Boolean = false,
    /**
     * 동의 장에서 받아야 하는 문서. **기본은 모두 해제**다.
     *
     * 이미 동의한 항목은 여기 없다 — 화면에서 해제해도 서버 동의는 철회되지 않으므로, 되돌릴 수
     * 없는 것을 되돌릴 수 있는 것처럼 보여 주지 않는다.
     */
    val consentDocuments: List<TermDocument> = emptyList(),
    val checkedConsents: Set<TermType> = emptySet(),
    val isConsentSubmitting: Boolean = false,
    val consentErrorMessage: String? = null,
) : UiState {
    /** 필수 동의는 하나도 빠질 수 없다. 서버가 세 종류를 모두 요구한다. */
    val canSubmitConsent: Boolean
        get() =
            consentDocuments.isNotEmpty() &&
                !isConsentSubmitting &&
                consentDocuments.all { it.termType in checkedConsents }
}
