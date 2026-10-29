package com.soma369.laimory.reminder

import java.time.LocalDate

/** 리마인더 문구를 고르고 알림을 띄운다. 실제 구현은 [DailyReminderNotifier]. */
interface DailyReminderNotifications {
    fun pickGeneral(lastIndex: Int?): DailyReminderCopy.General

    fun pickCompleted(lastIndex: Int?): DailyReminderCopy.Completed

    /**
     * 알림을 띄운다. 기기가 알림을 표시할 수 없으면 아무것도 하지 않고 `false`.
     *
     * @param recordDate 완료 문구일 때 누르면 열 기록 날짜.
     */
    fun notify(
        copy: DailyReminderCopy,
        recordDate: LocalDate,
    ): Boolean
}
