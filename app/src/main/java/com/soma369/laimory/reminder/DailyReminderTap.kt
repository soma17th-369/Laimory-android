package com.soma369.laimory.reminder

import android.content.Intent
import java.time.LocalDate

/**
 * 리마인더 알림을 눌러 MainActivity 로 들어올 때 싣는 값.
 *
 * 일반 문구는 홈으로, 완료 문구는 그날 타임라인(저장된 기록이라 읽기 모드)으로 연다.
 */
object DailyReminderTap {
    private const val EXTRA_OPENED = "daily_reminder_opened"
    private const val EXTRA_RECORD_DATE = "daily_reminder_record_date"

    /** 알림이 열 곳. [recordDate] 가 있으면 그날 타임라인, 없으면 홈이다. */
    data class Destination(
        val recordDate: LocalDate?,
    )

    fun put(
        intent: Intent,
        recordDate: LocalDate?,
    ): Intent =
        intent.putExtra(EXTRA_OPENED, true).apply {
            recordDate?.let { putExtra(EXTRA_RECORD_DATE, it.toString()) }
        }

    /** 리마인더로 들어온 것이면 목적지를 꺼내고 인텐트에서 지운다 — 재생성 때 다시 처리하지 않게. */
    fun consume(intent: Intent): Destination? {
        if (!intent.getBooleanExtra(EXTRA_OPENED, false)) return null
        val recordDate =
            intent.getStringExtra(EXTRA_RECORD_DATE)?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
        intent.removeExtra(EXTRA_OPENED)
        intent.removeExtra(EXTRA_RECORD_DATE)
        return Destination(recordDate)
    }
}
