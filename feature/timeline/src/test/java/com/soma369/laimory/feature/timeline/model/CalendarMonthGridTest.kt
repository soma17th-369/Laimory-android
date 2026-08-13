package com.soma369.laimory.feature.timeline.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarMonthGridTest {
    @Test
    fun `일요일에 시작하는 평년 2월은 앞뒤 빈 칸 없이 4주로 떨어진다`() {
        // 2026-02-01 은 일요일이고 2026 은 평년이라 28일 = 정확히 4주다.
        val grid = YearMonth.of(2026, 2).toCalendarMonthGrid()

        assertEquals(4, grid.weeks.size)
        assertEquals(LocalDate.of(2026, 2, 1), grid.weeks.first().first())
        assertEquals(LocalDate.of(2026, 2, 28), grid.weeks.last().last())
        assertEquals(28, grid.weeks.flatten().count { it != null })
    }

    @Test
    fun `윤년 2월은 29일까지 격자에 담긴다`() {
        val grid = YearMonth.of(2028, 2).toCalendarMonthGrid()

        assertEquals(29, grid.weeks.flatten().count { it != null })
        assertEquals(LocalDate.of(2028, 2, 29), grid.weeks.flatten().filterNotNull().last())
    }

    @Test
    fun `금요일에 시작하는 31일 월은 6주가 된다`() {
        // Figma 기준 화면과 같은 달 — 2026-05-01 은 금요일이다.
        val grid = YearMonth.of(2026, 5).toCalendarMonthGrid()

        assertEquals(6, grid.weeks.size)
        val firstWeek = grid.weeks.first()
        // 일~목은 빈 칸, 금·토에 1·2일이 놓인다.
        assertEquals(List(5) { null }, firstWeek.take(5))
        assertEquals(LocalDate.of(2026, 5, 1), firstWeek[5])
        assertEquals(LocalDate.of(2026, 5, 2), firstWeek[6])
        // 마지막 주는 31일 하나만 채워지고 나머지는 빈 칸이다.
        assertEquals(LocalDate.of(2026, 5, 31), grid.weeks.last().first())
        assertEquals(List(6) { null }, grid.weeks.last().drop(1))
    }

    @Test
    fun `모든 주는 7칸이고 이웃 월 날짜는 채우지 않는다`() {
        (1..12).forEach { month ->
            val yearMonth = YearMonth.of(2026, month)
            val grid = yearMonth.toCalendarMonthGrid()

            grid.weeks.forEach { week -> assertEquals(DAYS_IN_WEEK, week.size) }
            assertEquals(yearMonth.lengthOfMonth(), grid.weeks.flatten().count { it != null })
            grid.weeks.flatten().filterNotNull().forEach { date ->
                assertEquals(yearMonth, YearMonth.from(date))
            }
        }
    }

    @Test
    fun `일요일 열은 항상 index 0 이다`() {
        (1..12).forEach { month ->
            val grid = YearMonth.of(2026, month).toCalendarMonthGrid()

            grid.weeks.forEach { week ->
                week.forEachIndexed { index, date ->
                    if (date != null) assertEquals(index, date.dayOfWeek.value % DAYS_IN_WEEK)
                }
            }
        }
    }

    @Test
    fun `12월과 1월 경계에서도 각 월의 날짜만 담긴다`() {
        val december = YearMonth.of(2026, 12).toCalendarMonthGrid()
        val january = YearMonth.of(2027, 1).toCalendarMonthGrid()

        assertEquals(YearMonth.of(2026, 12), december.month)
        assertEquals(YearMonth.of(2027, 1), january.month)
        assertTrue(december.weeks.flatten().filterNotNull().all { it.monthValue == 12 && it.year == 2026 })
        assertTrue(january.weeks.flatten().filterNotNull().all { it.monthValue == 1 && it.year == 2027 })
    }
}
