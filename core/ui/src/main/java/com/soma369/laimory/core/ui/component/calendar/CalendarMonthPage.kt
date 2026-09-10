package com.soma369.laimory.core.ui.component.calendar

import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 월간 pager 의 페이지 번호 ↔ 월 변환.
 *
 * 다룰 수 있는 연도 범위를 페이지 범위 자체로 삼는다. 이동한 뒤에 범위 밖 월을 걸러내는 대신
 * 이동할 수 있는 페이지를 그 범위로 끝내 두면 범위 밖 월이 애초에 만들어지지 않는다.
 */
fun YearMonth.toPagerPage(): Int = ChronoUnit.MONTHS.between(CALENDAR_FIRST_MONTH, this).toInt()

fun monthOfPagerPage(page: Int): YearMonth = CALENDAR_FIRST_MONTH.plusMonths(page.toLong())

/** 표시 가능한 범위로 자른다. 접근성 액션·연월 피커처럼 pager 밖에서 들어오는 이동에 쓴다. */
fun YearMonth.coerceToCalendarRange(): YearMonth =
    when {
        isBefore(CALENDAR_FIRST_MONTH) -> CALENDAR_FIRST_MONTH
        isAfter(CALENDAR_LAST_MONTH) -> CALENDAR_LAST_MONTH
        else -> this
    }

/** 달력이 다룰 수 있는 연도 범위. 네 자리 연도를 벗어나면 월별 조회가 받지 못한다. */
val CALENDAR_YEAR_RANGE = 1000..9999

val CALENDAR_FIRST_MONTH: YearMonth = YearMonth.of(CALENDAR_YEAR_RANGE.first, 1)
val CALENDAR_LAST_MONTH: YearMonth = YearMonth.of(CALENDAR_YEAR_RANGE.last, 12)

/** 첫 월이 0 페이지라 페이지 수는 범위 안의 월 수와 같다. */
val MONTH_PAGE_COUNT: Int = CALENDAR_FIRST_MONTH.toPageCountUntil(CALENDAR_LAST_MONTH)

private fun YearMonth.toPageCountUntil(last: YearMonth): Int = (ChronoUnit.MONTHS.between(this, last) + 1).toInt()
