package com.soma369.laimory.feature.terms.state

import com.soma369.laimory.core.ui.base.UiSideEffect

/**
 * 동의 성공은 side effect 가 없다 — 판정이 통과로 바뀌면 앱 루트가 스스로 다음 화면으로 간다.
 * 화면이 이동을 직접 지시하면 루트 판정과 두 곳에서 같은 결정을 하게 된다.
 */
sealed interface TermsUiSideEffect : UiSideEffect
