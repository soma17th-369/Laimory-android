package com.soma369.laimory.feature.collection.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface HealthUiIntent : UiIntent {
    /** 건강 데이터를 수집한다(최근 한 달, 걸음수·수면). */
    data object Collect : HealthUiIntent

    /** 스테이징된 건강 데이터를 모두 비운다(일괄 삭제). */
    data object ClearStaged : HealthUiIntent
}
