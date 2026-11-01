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
    fun `오늘 이미 받았으면 시각을 늦춰도 내일 건다`() {
        // 21시에 받고 22시로 옮긴 경우. 오늘 22시에 걸면 띄우지도 않을 알람으로 기기만 깨운다.
        val now = ZonedDateTime.of(2026, 10, 29, 21, 30, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 10, 30, 22, 0, 0, 0, zone),
            DailyReminderSchedule.nextTriggerAt(now, LocalTime.of(22, 0), deliveredRecordDate = LocalDate.of(2026, 10, 29)),
        )
    }

    @Test
    fun `새벽으로 옮겨도 같은 기록 날짜면 하루 민다`() {
        // 21시에 받고 01시로 옮기면 다음 01시는 06시 전이라 아직 같은 하루다.
        val now = ZonedDateTime.of(2026, 10, 29, 21, 30, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 10, 31, 1, 0, 0, 0, zone),
            DailyReminderSchedule.nextTriggerAt(now, LocalTime.of(1, 0), deliveredRecordDate = LocalDate.of(2026, 10, 29)),
        )
    }

    @Test
    fun `받은 적 없는 기록 날짜면 그대로 건다`() {
        val now = ZonedDateTime.of(2026, 10, 29, 21, 30, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 10, 29, 22, 0, 0, 0, zone),
            DailyReminderSchedule.nextTriggerAt(now, LocalTime.of(22, 0), deliveredRecordDate = LocalDate.of(2026, 10, 28)),
        )
    }

    @Test
    fun `오늘 아직 안 받았으면 지난 시간대로 바꿔도 지금 건다`() {
        // 23시에 21시로 바꾼 경우. 내일로 미루면 오늘 하루를 통째로 거른다.
        val now = ZonedDateTime.of(2026, 10, 29, 23, 0, 0, 0, zone)

        assertEquals(now, DailyReminderSchedule.nextTriggerAt(now, nine, deliveredRecordDate = null, catchUpToday = true))
    }

    @Test
    fun `오늘 이미 받았으면 따라잡지 않고 내일 건다`() {
        val now = ZonedDateTime.of(2026, 10, 29, 23, 0, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 10, 30, 21, 0, 0, 0, zone),
            DailyReminderSchedule.nextTriggerAt(now, nine, deliveredRecordDate = LocalDate.of(2026, 10, 29), catchUpToday = true),
        )
    }

    @Test
    fun `따라잡기여도 아직 오지 않은 시각이면 그 시각에 건다`() {
        val now = ZonedDateTime.of(2026, 10, 29, 20, 0, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 10, 29, 21, 0, 0, 0, zone),
            DailyReminderSchedule.nextTriggerAt(now, nine, deliveredRecordDate = LocalDate.of(2026, 10, 28), catchUpToday = true),
        )
    }

    @Test
    fun `새벽에 바꾸면 어제 기록 기준으로 따라잡는다`() {
        // 01시는 아직 어제(10/28) 기록이다. 어제 몫을 받지 않았고 23시는 이미 지났다.
        val now = ZonedDateTime.of(2026, 10, 29, 1, 0, 0, 0, zone)

        assertEquals(
            now,
            DailyReminderSchedule.nextTriggerAt(
                now,
                LocalTime.of(23, 0),
                deliveredRecordDate = LocalDate.of(2026, 10, 27),
                catchUpToday = true,
            ),
        )
    }

    @Test
    fun `새벽 시각으로 바꾸면 그 하루의 새벽이 오기 전엔 기다린다`() {
        // 23시에 01시로 바꾸면 오늘(10/29) 기록의 01시는 달력으로 10/30 01시라 아직 오지 않았다.
        val now = ZonedDateTime.of(2026, 10, 29, 23, 0, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 10, 30, 1, 0, 0, 0, zone),
            DailyReminderSchedule.nextTriggerAt(now, LocalTime.of(1, 0), deliveredRecordDate = null, catchUpToday = true),
        )
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
