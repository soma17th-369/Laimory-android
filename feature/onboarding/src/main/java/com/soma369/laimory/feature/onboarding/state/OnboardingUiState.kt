package com.soma369.laimory.feature.onboarding.state

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
) : UiState
