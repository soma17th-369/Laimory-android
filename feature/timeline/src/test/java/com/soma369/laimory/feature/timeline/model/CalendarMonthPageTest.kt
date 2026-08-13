package com.soma369.laimory.feature.timeline.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class CalendarMonthPageTest {
    @Test
    fun `기준 월은 가운데 페이지에 놓인다`() {
        assertEquals(MONTH_PAGE_CENTER, PAGER_EPOCH_MONTH.toPagerPage())
        assertEquals(PAGER_EPOCH_MONTH, monthOfPagerPage(MONTH_PAGE_CENTER))
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
    fun `양 끝 페이지도 넘치지 않고 월로 바뀐다`() {
        // Int 범위 전체가 유효한 페이지여야 스크롤이 끝에서 막히지 않는다.
        assertTrue(monthOfPagerPage(0).year < PAGER_EPOCH_MONTH.year)
        assertTrue(monthOfPagerPage(Int.MAX_VALUE).year > PAGER_EPOCH_MONTH.year)
        assertEquals(MONTH_PAGE_COUNT, Int.MAX_VALUE)
    }
}
