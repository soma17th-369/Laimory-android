package com.soma369.laimory.feature.collection.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface CalendarUiIntent : UiIntent {
    /** 개인 캘린더 일정을 수집한다(최근 한 달). */
    data object Collect : CalendarUiIntent

    /** 스테이징된 일정을 모두 비운다(일괄 삭제). */
    data object ClearStaged : CalendarUiIntent
}
