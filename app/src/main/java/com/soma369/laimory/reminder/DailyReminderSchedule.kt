package com.soma369.laimory.reminder

import com.soma369.laimory.core.domain.model.reminder.DailyReminderRecordDay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

/** 일일 리마인더의 시각 계산. Android 의존이 없어 JVM 테스트로 검증한다. */
internal object DailyReminderSchedule {
    /**
     * 늦게 깨어난 알람을 버리는 한도.
     *
     * 절전 중 알람은 미뤄질 수 있다. 저녁 알림이 자정 넘어 오면 "오늘"이 무엇인지 어긋나므로 띄우지 않는다.
     */
    const val LATE_LIMIT_MINUTES = 120L

    /**
     * [now] 이후 처음 오는 [time]. 지금이 바로 그 시각이면 다음 날이다 — 방금 띄운 알람이 다시 걸리지 않게.
     *
     * 그 시각이 가리키는 기록 날짜에 이미 알림을 띄웠으면([deliveredRecordDate]) 하루 더 민다. 21시에 받고 22시로
     * 옮기면 오늘 22시가 아니라 내일 22시에 건다 — 걸어 두면 띄우지도 않을 알람으로 기기만 깨운다.
     *
     * [catchUpToday] 면 오늘 몫을 놓치지 않게 한다. 오늘(기록 날짜) 아직 받지 않았는데 고른 시각이 이미 지났으면
     * 내일로 미루지 않고 [now] 에 건다. 23시에 21시로 바꾼 사람은 오늘 알림을 받아야 한다 — 시간대를 바꿨다는 이유로
     * 하루를 통째로 거르면 설정이 고장 난 것처럼 보인다.
     */
    fun nextTriggerAt(
        now: ZonedDateTime,
        time: LocalTime,
        deliveredRecordDate: LocalDate? = null,
        catchUpToday: Boolean = false,
    ): ZonedDateTime {
        if (catchUpToday) {
            val recordDate = recordDateAt(now.toLocalDateTime())
            if (recordDate != deliveredRecordDate && !occurrenceOn(recordDate, time, now).isAfter(now)) return now
        }
        val today = ZonedDateTime.of(now.toLocalDate(), time, now.zone)
        val first = if (today.isAfter(now)) today else ZonedDateTime.of(now.toLocalDate().plusDays(1), time, now.zone)
        return if (recordDateAt(first.toLocalDateTime()) == deliveredRecordDate) {
            ZonedDateTime.of(first.toLocalDate().plusDays(1), time, now.zone)
        } else {
            first
        }
    }

    /** [scheduledAt] 에 띄우는 알림이 가리키는 기록 날짜. */
    fun recordDateAt(scheduledAt: LocalDateTime): LocalDate = DailyReminderRecordDay.of(scheduledAt)

    fun isTooLate(
        scheduledAt: LocalDateTime,
        now: LocalDateTime,
    ): Boolean = now.isAfter(scheduledAt.plusMinutes(LATE_LIMIT_MINUTES))

    /** [recordDate] 하루 안에서 [time] 이 오는 때. 하루 시작 전 시각(새벽)은 달력으로 다음 날이다. */
    private fun occurrenceOn(
        recordDate: LocalDate,
        time: LocalTime,
        now: ZonedDateTime,
    ): ZonedDateTime {
        val date = if (time.isBefore(DailyReminderRecordDay.START)) recordDate.plusDays(1) else recordDate
        return ZonedDateTime.of(date, time, now.zone)
    }
}
