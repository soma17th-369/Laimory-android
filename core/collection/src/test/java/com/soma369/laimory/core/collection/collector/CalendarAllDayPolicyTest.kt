package com.soma369.laimory.core.collection.collector

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal class CalendarAllDayPolicyTest {
    private val seoul: ZoneId = ZoneId.of("Asia/Seoul")

    /** 캘린더가 종일 일정 BEGIN/END 로 적는 "UTC 자정" 값. */
    private fun utcMidnightMillis(date: LocalDate): Long = Instant.parse("${date}T00:00:00Z").toEpochMilli()

    @Test
    fun `종일 일정의 UTC 자정 날짜 표기를 기기 시간대 자정으로 바꾼다`() {
        val day = LocalDate.of(2026, 10, 15)

        assertEquals(
            Instant.parse("2026-10-14T15:00:00Z"),
            CalendarAllDayPolicy.toZonedMidnight(utcMidnightMillis(day), seoul),
        )
        assertEquals(
            Instant.parse("2026-10-15T15:00:00Z"),
            CalendarAllDayPolicy.toZonedMidnight(utcMidnightMillis(day.plusDays(1)), seoul),
        )
    }

    @Test
    fun `UTC 보다 늦은 시간대에서도 같은 날짜의 자정으로 잡는다`() {
        val newYork = ZoneId.of("America/New_York")
        val day = LocalDate.of(2026, 10, 15)

        assertEquals(
            day.atStartOfDay(newYork).toInstant(),
            CalendarAllDayPolicy.toZonedMidnight(utcMidnightMillis(day), newYork),
        )
    }

    @Test
    fun `서머타임 전환일에도 그 날짜의 자정으로 잡는다`() {
        val newYork = ZoneId.of("America/New_York")
        // 2026-03-08 02:00 에 서머타임이 시작돼 이날은 23시간이다.
        val day = LocalDate.of(2026, 3, 8)

        assertEquals(
            Instant.parse("2026-03-08T05:00:00Z"),
            CalendarAllDayPolicy.toZonedMidnight(utcMidnightMillis(day), newYork),
        )
        assertEquals(
            Instant.parse("2026-03-09T04:00:00Z"),
            CalendarAllDayPolicy.toZonedMidnight(utcMidnightMillis(day.plusDays(1)), newYork),
        )
    }
}
