package com.soma369.laimory.feature.collection.screen.sleep

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SleepInputMathTest {
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val wakeDate: LocalDate = LocalDate.of(2026, 7, 9)

    @Test
    fun `취침이 기상보다 늦으면 취침은 전날, 밤을 넘겨 8시간`() {
        val (start, end) =
            SleepInputMath.sleepInstants(wakeDate, bedTime = LocalTime.of(23, 0), wakeTime = LocalTime.of(7, 0), zone = zone)

        // 취침 2026-07-08 23:00 KST, 기상 2026-07-09 07:00 KST
        assertEquals(LocalDate.of(2026, 7, 8).atTime(23, 0).atZone(zone).toInstant(), start)
        assertEquals(wakeDate.atTime(7, 0).atZone(zone).toInstant(), end)
        assertEquals(480, SleepInputMath.durationMinutes(wakeDate, LocalTime.of(23, 0), LocalTime.of(7, 0), zone))
    }

    @Test
    fun `취침이 기상보다 이르면 같은 날 새벽 취침, 7시간`() {
        val (start, end) =
            SleepInputMath.sleepInstants(wakeDate, bedTime = LocalTime.of(4, 20), wakeTime = LocalTime.of(11, 20), zone = zone)

        assertEquals(wakeDate.atTime(4, 20).atZone(zone).toInstant(), start)
        assertEquals(wakeDate.atTime(11, 20).atZone(zone).toInstant(), end)
        assertEquals(420, SleepInputMath.durationMinutes(wakeDate, LocalTime.of(4, 20), LocalTime.of(11, 20), zone))
    }
}
