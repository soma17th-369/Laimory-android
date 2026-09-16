package com.soma369.laimory.feature.home.state

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HomeDefaultDateTest {
    private val date = LocalDate.of(2026, 9, 17)

    @Test
    fun `06시 전에는 어제를 기본 날짜로 둔다`() {
        assertEquals(date.minusDays(1), HomeDefaultDate.of(date.atTime(0, 0)))
        assertEquals(date.minusDays(1), HomeDefaultDate.of(date.atTime(5, 59, 59)))
    }

    @Test
    fun `06시부터는 오늘을 기본 날짜로 둔다`() {
        assertEquals(date, HomeDefaultDate.of(date.atTime(6, 0)))
        assertEquals(date, HomeDefaultDate.of(date.atTime(23, 59)))
    }

    @Test
    fun `06시 전이면 다음 변경은 그날 06시다`() {
        assertEquals(date.atTime(6, 0), HomeDefaultDate.nextChangeAfter(date.atTime(0, 0)))
        assertEquals(date.atTime(6, 0), HomeDefaultDate.nextChangeAfter(date.atTime(3, 30)))
    }

    @Test
    fun `06시부터는 다음 변경이 다음 자정이다`() {
        // 자정에 기본 날짜는 그대로지만 CTA 부제의 오늘·어제가 바뀐다.
        val nextMidnight: LocalDateTime = date.plusDays(1).atStartOfDay()
        assertEquals(nextMidnight, HomeDefaultDate.nextChangeAfter(date.atTime(6, 0)))
        assertEquals(nextMidnight, HomeDefaultDate.nextChangeAfter(date.atTime(23, 59)))
    }
}
