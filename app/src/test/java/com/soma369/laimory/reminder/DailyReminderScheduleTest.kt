package com.soma369.laimory.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyReminderScheduleTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val nine = LocalTime.of(21, 0)

    @Test
    fun `아직 그 시각 전이면 오늘 건다`() {
        val now = ZonedDateTime.of(2026, 10, 29, 20, 59, 0, 0, zone)

        assertEquals(ZonedDateTime.of(2026, 10, 29, 21, 0, 0, 0, zone), DailyReminderSchedule.nextTriggerAt(now, nine))
    }

    @Test
    fun `바로 그 시각이면 다음 날 건다`() {
        // 방금 울린 알람이 다시 걸리면 같은 알림이 연달아 온다.
        val now = ZonedDateTime.of(2026, 10, 29, 21, 0, 0, 0, zone)

        assertEquals(ZonedDateTime.of(2026, 10, 30, 21, 0, 0, 0, zone), DailyReminderSchedule.nextTriggerAt(now, nine))
    }

    @Test
    fun `지났으면 다음 날 건다`() {
        val now = ZonedDateTime.of(2026, 10, 29, 23, 30, 0, 0, zone)

        assertEquals(ZonedDateTime.of(2026, 10, 30, 21, 0, 0, 0, zone), DailyReminderSchedule.nextTriggerAt(now, nine))
    }

    @Test
    fun `저녁 알림은 그날 기록을 가리킨다`() {
        assertEquals(
            LocalDate.of(2026, 10, 29),
            DailyReminderSchedule.recordDateAt(LocalDateTime.of(2026, 10, 29, 21, 0)),
        )
    }

    @Test
    fun `새벽 알림은 어제 기록을 가리킨다`() {
        // 홈의 기본 날짜 규칙과 같다 — 06시 전은 아직 어제다.
        assertEquals(
            LocalDate.of(2026, 10, 28),
            DailyReminderSchedule.recordDateAt(LocalDateTime.of(2026, 10, 29, 1, 0)),
        )
        assertEquals(
            LocalDate.of(2026, 10, 29),
            DailyReminderSchedule.recordDateAt(LocalDateTime.of(2026, 10, 29, 6, 0)),
        )
    }

    @Test
    fun `두 시간 넘게 늦은 알람은 버린다`() {
        val scheduledAt = LocalDateTime.of(2026, 10, 29, 21, 0)

        assertFalse(DailyReminderSchedule.isTooLate(scheduledAt, scheduledAt.plusMinutes(120)))
        assertTrue(DailyReminderSchedule.isTooLate(scheduledAt, scheduledAt.plusMinutes(121)))
    }
}
