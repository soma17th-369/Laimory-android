package com.soma369.laimory.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** 리마인더 알람 수신. 우리 PendingIntent 로만 오므로 exported=false. */
@AndroidEntryPoint
class DailyReminderAlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var delivery: DailyReminderDelivery

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val scheduledAtMillis =
            intent.getLongExtra(EXTRA_SCHEDULED_AT_MILLIS, MISSING).takeIf { it != MISSING }
        val pending = goAsync()
        delivery.onAlarm(scheduledAtMillis, onFinished = pending::finish)
    }

    companion object {
        /** 알람이 원래 울려야 했던 시각. 늦게 온 알람을 가리고 그날 기록 날짜를 정한다. */
        const val EXTRA_SCHEDULED_AT_MILLIS = "scheduled_at_millis"
        private const val MISSING = -1L
    }
}
