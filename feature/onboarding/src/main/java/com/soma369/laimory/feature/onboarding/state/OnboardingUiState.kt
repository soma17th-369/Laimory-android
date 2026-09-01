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
    /**
     * 체크 표시 상태. 사용자가 켜는 것이 아니라 **`모두 동의하고 시작하기` 를 누르면 채워진다.**
     *
     * 목록은 무엇에 동의하는지 보여 주는 자리이고 동의 행위는 버튼이다 — 결과가 분명한 버튼으로
     * 받는 편이 항목마다 체크를 요구하는 것보다 의사가 또렷하다.
     */
    val checkedConsents: Set<TermType> = emptySet(),
    /**
     * 이미 동의해 되돌릴 수 없는 항목.
     *
     * 화면에서 지우지 않고 **체크된 채로 남긴다** — 목록에서 빼면 무엇에 동의하고 시작하는지
     * 알 수 없고, 처음 보는 사용자와 다시 온 사용자가 서로 다른 화면을 보게 된다. 앱이 동의를
     * 철회시킬 수는 없으므로 끄지는 못한다.
     */
    val lockedConsents: Set<TermType> = emptySet(),
    val isConsentSubmitting: Boolean = false,
    val consentErrorMessage: String? = null,
) : UiState
