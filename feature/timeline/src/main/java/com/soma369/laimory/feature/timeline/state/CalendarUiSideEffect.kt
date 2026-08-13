package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.ui.base.UiSideEffect

/**
 * 캘린더는 현재 별도 UI SideEffect 를 발행하지 않는다.
 *
 * 타임라인 기록 화면 진입은 [com.soma369.laimory.core.domain.helper.NavigationHelper] 로 나가고,
 * 조회 실패 안내는 BaseMviViewModel 의 공통 스낵바 채널을 쓴다.
 */
sealed interface CalendarUiSideEffect : UiSideEffect
