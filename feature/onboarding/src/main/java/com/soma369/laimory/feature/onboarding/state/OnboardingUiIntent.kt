package com.soma369.laimory.feature.onboarding.state

import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.ui.base.UiIntent

sealed interface OnboardingUiIntent : UiIntent {
    /** 사용자가 장을 넘겼다. 진행 상태를 기록한다. */
    data class PageChanged(val pageIndex: Int) : OnboardingUiIntent

    /** 마지막 장의 완료 CTA. 저장이 끝나야 Home 으로 간다. */
    data object Complete : OnboardingUiIntent

    /** 백그라운드 위치까지 허용됐다. 자동 수집을 켠다. */
    data object EnableLocationTracking : OnboardingUiIntent

    /** 동의 장의 항목 하나를 켜고 끈다. */
    data class ConsentToggled(val termType: TermType) : OnboardingUiIntent

    /** 동의 장의 CTA. 등록에 성공해야 다음 장으로 간다. */
    data object SubmitConsent : OnboardingUiIntent
}
