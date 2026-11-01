package com.soma369.laimory.reminder

import java.time.LocalDate
import java.time.LocalTime

/** 리마인더 알람을 걸고 내린다. 실제 구현은 [DailyReminderAlarmScheduler]. */
interface DailyReminderAlarms {
    /**
     * @param deliveredRecordDate 이미 알림을 띄운 기록 날짜. 그 날을 가리키는 시각은 건너뛴다.
     * @param catchUpToday 오늘 아직 받지 않았으면 지난 시각이어도 지금 건다. 사용자가 시간대를 바꿨을 때만 켠다.
     */
    fun schedule(
        time: LocalTime,
        deliveredRecordDate: LocalDate?,
        catchUpToday: Boolean = false,
    )

    fun cancel()
}
