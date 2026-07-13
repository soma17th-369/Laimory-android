package com.soma369.laimory.feature.collection.screen.sleep

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * 수면 입력의 시각 계산(순수).
 *
 * 취침이 기상보다 늦거나 같으면(예: 취침 23:00, 기상 07:00) 취침은 [wakeDate] 전날로 본다.
 * 취침이 기상보다 이르면(예: 취침 04:20, 기상 11:20) 같은 날 새벽 취침으로 본다.
 */
internal object SleepInputMath {
    /** 취침~기상을 (start, end) instant 로. [wakeDate] 는 기상일. */
    fun sleepInstants(
        wakeDate: LocalDate,
        bedTime: LocalTime,
        wakeTime: LocalTime,
        zone: ZoneId,
    ): Pair<Instant, Instant> {
        val bedDate = if (bedTime >= wakeTime) wakeDate.minusDays(1) else wakeDate
        val start = bedDate.atTime(bedTime).atZone(zone).toInstant()
        val end = wakeDate.atTime(wakeTime).atZone(zone).toInstant()
        return start to end
    }

    /** 수면 길이(분). */
    fun durationMinutes(
        wakeDate: LocalDate,
        bedTime: LocalTime,
        wakeTime: LocalTime,
        zone: ZoneId,
    ): Long {
        val (start, end) = sleepInstants(wakeDate, bedTime, wakeTime, zone)
        return Duration.between(start, end).toMinutes()
    }
}
