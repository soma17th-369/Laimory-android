package com.soma369.laimory.feature.onboarding.state

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface OnboardingUiSideEffect : UiSideEffect {
    /** 마지막으로 본 장으로 Pager 를 옮긴다. 복원은 한 번만 일어난다. */
    data class RestorePage(val pageIndex: Int) : OnboardingUiSideEffect

    /** 동의가 기록됐다. Pager 를 다음 장으로 옮긴다 — 위치를 아는 쪽은 화면이다. */
    data object ConsentAccepted : OnboardingUiSideEffect
}
