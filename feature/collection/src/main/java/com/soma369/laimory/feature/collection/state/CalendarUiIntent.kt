package com.soma369.laimory.feature.collection.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface CalendarUiIntent : UiIntent {
    /** 개인 캘린더 일정을 시스템 캘린더에서 읽는다(최근 한 달). 저장하지 않는다. */
    data object Load : CalendarUiIntent
}
