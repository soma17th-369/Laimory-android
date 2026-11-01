package com.soma369.laimory.reminder

import android.content.Intent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsReminderCopy
import com.soma369.laimory.core.domain.model.analytics.AnalyticsReminderKind
import java.time.LocalDate

/**
 * 리마인더 알림을 눌러 MainActivity 로 들어올 때 싣는 값.
 *
 * 일반 문구는 홈으로, 완료 문구는 그날 타임라인(저장된 기록이라 읽기 모드)으로 연다.
 */
object DailyReminderTap {
    private const val EXTRA_OPENED = "daily_reminder_opened"
    private const val EXTRA_RECORD_DATE = "daily_reminder_record_date"
    private const val EXTRA_COPY_KIND = "daily_reminder_copy_kind"
    private const val EXTRA_COPY_TITLE = "daily_reminder_copy_title"

    /**
     * 알림이 열 곳. [recordDate] 가 있으면 그날 타임라인, 없으면 홈이다.
     *
     * [copy] 는 그 알림에 보인 문구다(분석용). 문구를 싣기 전에 띄운 알림이면 없다.
     */
    data class Destination(
        val recordDate: LocalDate?,
        val copy: AnalyticsReminderCopy?,
    )

    fun put(
        intent: Intent,
        recordDate: LocalDate?,
        copy: AnalyticsReminderCopy,
    ): Intent =
        intent.putExtra(EXTRA_OPENED, true).apply {
            recordDate?.let { putExtra(EXTRA_RECORD_DATE, it.toString()) }
            putExtra(EXTRA_COPY_KIND, copy.kind.name)
            putExtra(EXTRA_COPY_TITLE, copy.title)
        }

    /** 리마인더로 들어온 것이면 목적지를 꺼내고 인텐트에서 지운다 — 재생성 때 다시 처리하지 않게. */
    fun consume(intent: Intent): Destination? {
        if (!intent.getBooleanExtra(EXTRA_OPENED, false)) return null
        val recordDate =
            intent.getStringExtra(EXTRA_RECORD_DATE)?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
        val copy =
            AnalyticsReminderKind.entries
                .firstOrNull { it.name == intent.getStringExtra(EXTRA_COPY_KIND) }
                ?.let { kind -> intent.getStringExtra(EXTRA_COPY_TITLE)?.let { title -> AnalyticsReminderCopy(kind, title) } }
        intent.removeExtra(EXTRA_OPENED)
        intent.removeExtra(EXTRA_RECORD_DATE)
        intent.removeExtra(EXTRA_COPY_KIND)
        intent.removeExtra(EXTRA_COPY_TITLE)
        return Destination(recordDate, copy)
    }
}
