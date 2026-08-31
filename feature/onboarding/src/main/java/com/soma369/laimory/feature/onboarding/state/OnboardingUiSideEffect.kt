package com.soma369.laimory.feature.onboarding.state

import com.soma369.laimory.core.ui.base.UiSideEffect

/**
 * 온보딩은 일회성 효과가 없다.
 *
 * 장 복원은 효과가 아니라 상태로 준다 — 효과로 밀면 Pager 가 첫 장으로 만들어진 뒤에 도착해
 * 화면이 한 번 튄다.
 */
sealed interface OnboardingUiSideEffect : UiSideEffect
