package com.soma369.laimory.feature.timeline.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.YearMonth

/** 일요일 시작 7열 월간 격자. 표시 월에 속하지 않는 칸은 null(빈 칸)이다. */
@Immutable
data class CalendarMonthGrid(
    val month: YearMonth,
    val weeks: List<List<LocalDate?>>,
)

/**
 * 표시 월을 일요일 시작 주 격자로 편다.
 *
 * 앞뒤 이웃 월 날짜는 채우지 않고 빈 칸으로 남긴다(Figma 기준). 행 수는 월 시작 요일과 일수에 따라
 * 4~6주로 달라진다 — 일요일에 시작하는 평년 2월은 4주, 금·토에 시작하는 31일 월은 6주다.
 */
fun YearMonth.toCalendarMonthGrid(): CalendarMonthGrid {
    // DayOfWeek 는 월요일이 1, 일요일이 7이다. 일요일 시작 격자에서 일요일 열 index 는 0 이어야 한다.
    val leadingBlanks = atDay(1).dayOfWeek.value % DAYS_IN_WEEK
    val cells: List<LocalDate?> = List<LocalDate?>(leadingBlanks) { null } + (1..lengthOfMonth()).map { atDay(it) }
    val trailingBlanks = (DAYS_IN_WEEK - cells.size % DAYS_IN_WEEK) % DAYS_IN_WEEK
    return CalendarMonthGrid(
        month = this,
        weeks = (cells + List(trailingBlanks) { null }).chunked(DAYS_IN_WEEK),
    )
}

/** 격자 열 수. 요일 헤더와 주 격자가 같은 값을 공유해야 열이 어긋나지 않는다. */
const val DAYS_IN_WEEK = 7
