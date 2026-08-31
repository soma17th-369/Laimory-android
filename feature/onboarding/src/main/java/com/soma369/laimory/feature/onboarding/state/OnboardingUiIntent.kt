package com.soma369.laimory.feature.onboarding.state

import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.ui.base.UiIntent

sealed interface OnboardingUiIntent : UiIntent {
    /** 사용자가 장을 넘겼다. 진행 상태를 기록한다. */
    data class PageChanged(val pageIndex: Int) : OnboardingUiIntent

    /**
     * 마지막 장의 완료 CTA. 받을 동의가 있으면 먼저 기록하고, 저장이 끝나야 Home 으로 간다.
     */
    data object Complete : OnboardingUiIntent

    /**
     * 동의하지 않고 온보딩만 끝낸다.
     *
     * 서버 gate 가 초안 생성에만 걸려 있어 거부해도 열람·편집은 그대로 쓸 수 있다. 나중에 초안을
     * 만들 때 그 화면이 다시 받는다.
     */
    data object SkipConsent : OnboardingUiIntent

    /** 백그라운드 위치까지 허용됐다. 자동 수집을 켠다. */
    data object EnableLocationTracking : OnboardingUiIntent

    /** 동의 장의 항목 하나를 켜고 끈다. */
    data class ConsentToggled(val termType: TermType) : OnboardingUiIntent
}
