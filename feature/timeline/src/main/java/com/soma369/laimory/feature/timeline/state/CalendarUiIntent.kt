package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalDate
import java.time.YearMonth

sealed interface CalendarUiIntent : UiIntent {
    /** 진입·복귀(ON_RESUME) 시 오늘을 재계산하고 서버 기록을 재동기화한다. */
    data object Sync : CalendarUiIntent

    /** 조회 실패 상태에서의 명시적 다시 시도. */
    data object RetryLoad : CalendarUiIntent

    /** pager 가 정착한 월 또는 접근성 액션으로 옮긴 월. 선택 날짜는 건드리지 않는다. */
    data class ShowMonth(
        val month: YearMonth,
    ) : CalendarUiIntent

    /** 날짜 선택. 기록이 있는 날짜만 타임라인 기록 화면으로 진입한다. */
    data class SelectDate(
        val date: LocalDate,
    ) : CalendarUiIntent

    /** TopBar 월 표기를 눌러 연·월 피커를 연다. */
    data object OpenMonthPicker : CalendarUiIntent

    data object DismissMonthPicker : CalendarUiIntent

    /** 피커 안에서만 연도를 넘긴다. 표시 월은 아직 바뀌지 않는다. */
    data object ShowPreviousPickerYear : CalendarUiIntent

    data object ShowNextPickerYear : CalendarUiIntent

    /** 피커에서 월을 골라 표시 월을 옮긴다. */
    data class SelectMonth(
        val month: YearMonth,
    ) : CalendarUiIntent
}
