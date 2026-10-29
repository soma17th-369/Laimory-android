package com.soma369.laimory.reminder

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

/** 일일 리마인더의 시각 계산. Android 의존이 없어 JVM 테스트로 검증한다. */
internal object DailyReminderSchedule {
    /**
     * 하루가 시작되는 시각. 홈의 기본 날짜 규칙과 같다 — 새벽에 연 홈은 어제를 고른다.
     *
     * 알림 시각을 새벽으로 옮긴 사람에게 그 알림은 어제를 마무리하라는 뜻이다.
     */
    private val DAY_START: LocalTime = LocalTime.of(6, 0)

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
     */
    fun nextTriggerAt(
        now: ZonedDateTime,
        time: LocalTime,
        deliveredRecordDate: LocalDate? = null,
    ): ZonedDateTime {
        val today = ZonedDateTime.of(now.toLocalDate(), time, now.zone)
        val first = if (today.isAfter(now)) today else ZonedDateTime.of(now.toLocalDate().plusDays(1), time, now.zone)
        return if (recordDateAt(first.toLocalDateTime()) == deliveredRecordDate) {
            ZonedDateTime.of(first.toLocalDate().plusDays(1), time, now.zone)
        } else {
            first
        }
    }

    /** [scheduledAt] 에 띄우는 알림이 가리키는 기록 날짜. */
    fun recordDateAt(scheduledAt: LocalDateTime): LocalDate =
        if (scheduledAt.toLocalTime().isBefore(DAY_START)) {
            scheduledAt.toLocalDate().minusDays(1)
        } else {
            scheduledAt.toLocalDate()
        }

    fun isTooLate(
        scheduledAt: LocalDateTime,
        now: LocalDateTime,
    ): Boolean = now.isAfter(scheduledAt.plusMinutes(LATE_LIMIT_MINUTES))
}
