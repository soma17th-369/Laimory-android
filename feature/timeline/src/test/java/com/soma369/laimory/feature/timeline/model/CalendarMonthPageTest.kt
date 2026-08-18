package com.soma369.laimory.feature.timeline.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class CalendarMonthPageTest {
    @Test
    fun `범위의 첫 달이 0 페이지다`() {
        assertEquals(0, CALENDAR_FIRST_MONTH.toPagerPage())
        assertEquals(CALENDAR_FIRST_MONTH, monthOfPagerPage(0))
    }

    @Test
    fun `페이지 수는 범위 안의 월 수와 같아 마지막 페이지가 마지막 달이다`() {
        assertEquals(CALENDAR_YEAR_RANGE.count() * MONTHS_IN_YEAR, MONTH_PAGE_COUNT)
        assertEquals(MONTH_PAGE_COUNT - 1, CALENDAR_LAST_MONTH.toPagerPage())
        assertEquals(CALENDAR_LAST_MONTH, monthOfPagerPage(MONTH_PAGE_COUNT - 1))
    }

    @Test
    fun `이웃 페이지는 이웃 달이다`() {
        val month = YearMonth.of(2026, 5)
        val page = month.toPagerPage()

        assertEquals(YearMonth.of(2026, 4), monthOfPagerPage(page - 1))
        assertEquals(YearMonth.of(2026, 6), monthOfPagerPage(page + 1))
    }

    @Test
    fun `연 경계에서도 페이지가 한 칸씩 이어진다`() {
        val december = YearMonth.of(2026, 12)

        assertEquals(YearMonth.of(2027, 1), monthOfPagerPage(december.toPagerPage() + 1))
        assertEquals(YearMonth.of(2026, 11), monthOfPagerPage(december.toPagerPage() - 1))
    }

    @Test
    fun `월과 페이지는 왕복해도 그대로다`() {
        var month = YearMonth.of(1900, 1)
        while (month.year <= 2100) {
            assertEquals(month, monthOfPagerPage(month.toPagerPage()))
            month = month.plusMonths(1)
        }
    }

    @Test
    fun `범위 밖 월은 경계로 잘린다`() {
        assertEquals(CALENDAR_FIRST_MONTH, YearMonth.of(999, 12).coerceToCalendarRange())
        assertEquals(CALENDAR_LAST_MONTH, YearMonth.of(10000, 1).coerceToCalendarRange())
        assertEquals(YearMonth.of(2026, 5), YearMonth.of(2026, 5).coerceToCalendarRange())
    }

    private companion object {
        const val MONTHS_IN_YEAR = 12
    }
}
