package com.soma369.laimory.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 다음 리마인더 알람을 건다.
 *
 * 정확 알람 권한은 쓰지 않는다. `SCHEDULE_EXACT_ALARM` 은 Android 14 부터 새 설치에 기본 거부고,
 * `USE_EXACT_ALARM` 은 Play 정책상 알람·캘린더 앱 몫이다. 대신 성격이 다른 비정확 알람 둘을 같은 시각에 건다.
 *
 * - [AlarmManager.setWindow]: 깨어 있으면 정한 시각부터 [WINDOW_MILLIS] 안에 온다. 절전(Doze) 중에는 미뤄진다.
 * - [AlarmManager.setAndAllowWhileIdle]: 절전 중에도 온다. 대신 시각이 느슨하다(Android 12+ 최대 1시간).
 *
 * 먼저 온 쪽이 알림을 띄우고 다음 날로 둘 다 다시 건다 — 같은 PendingIntent 로 다시 걸면 이전 알람은 지워지므로
 * 늦은 쪽은 보통 오지 않는다. 그래도 겹치면 받는 쪽이 날짜로 걸러 한 번만 띄운다.
 */
@Singleton
class DailyReminderAlarmScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val alarmManager: AlarmManager
            get() = context.getSystemService(AlarmManager::class.java)

        fun schedule(time: LocalTime) {
            val triggerAt = DailyReminderSchedule.nextTriggerAt(ZonedDateTime.now(), time)
            val triggerAtMillis = triggerAt.toInstant().toEpochMilli()
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                WINDOW_MILLIS,
                pendingIntent(REQUEST_CODE_WINDOW, triggerAtMillis),
            )
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent(REQUEST_CODE_WHILE_IDLE, triggerAtMillis),
            )
        }

        fun cancel() {
            alarmManager.cancel(pendingIntent(REQUEST_CODE_WINDOW, triggerAtMillis = null))
            alarmManager.cancel(pendingIntent(REQUEST_CODE_WHILE_IDLE, triggerAtMillis = null))
        }

        /** 같은 요청 코드면 extras 가 달라도 같은 PendingIntent 로 본다. 취소는 extras 없이도 맞는다. */
        private fun pendingIntent(
            requestCode: Int,
            triggerAtMillis: Long?,
        ): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, DailyReminderAlarmReceiver::class.java).apply {
                    triggerAtMillis?.let { putExtra(DailyReminderAlarmReceiver.EXTRA_SCHEDULED_AT_MILLIS, it) }
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private companion object {
            const val REQUEST_CODE_WINDOW = 2101
            const val REQUEST_CODE_WHILE_IDLE = 2102

            /** 정한 시각 뒤로 허용하는 오차. 알림 목적에 충분히 가깝고, 시스템이 다른 알람과 묶을 여지를 준다. */
            const val WINDOW_MILLIS = 10 * 60 * 1000L
        }
    }
