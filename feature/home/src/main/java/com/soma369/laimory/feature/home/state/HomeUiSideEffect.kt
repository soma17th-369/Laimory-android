package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.ui.base.UiSideEffect

/** 홈은 현재 일회성 부수 효과가 없다. 필요해지면 하위 타입을 추가한다. */
sealed interface HomeUiSideEffect : UiSideEffect
