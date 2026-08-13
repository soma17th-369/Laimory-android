package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalDate

sealed interface CalendarUiIntent : UiIntent {
    /** 진입·복귀(ON_RESUME) 시 오늘을 재계산하고 서버 기록을 재동기화한다. */
    data object Sync : CalendarUiIntent

    /** 조회 실패 상태에서의 명시적 다시 시도. */
    data object RetryLoad : CalendarUiIntent

    data object ShowPreviousMonth : CalendarUiIntent

    data object ShowNextMonth : CalendarUiIntent

    /** 날짜 선택. 기록이 있는 날짜만 타임라인 기록 화면으로 진입한다. */
    data class SelectDate(
        val date: LocalDate,
    ) : CalendarUiIntent
}
