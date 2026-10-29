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

    /** [now] 이후 처음 오는 [time]. 지금이 바로 그 시각이면 다음 날이다 — 방금 띄운 알람이 다시 걸리지 않게. */
    fun nextTriggerAt(
        now: ZonedDateTime,
        time: LocalTime,
    ): ZonedDateTime {
        val today = ZonedDateTime.of(now.toLocalDate(), time, now.zone)
        return if (today.isAfter(now)) today else ZonedDateTime.of(now.toLocalDate().plusDays(1), time, now.zone)
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
