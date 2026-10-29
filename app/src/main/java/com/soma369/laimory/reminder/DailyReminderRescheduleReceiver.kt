package com.soma369.laimory.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 알람이 사라지거나 어긋나는 때 다시 건다 — 재부팅, 앱 업데이트, 기기 시각·시간대 변경.
 *
 * 모두 시스템만 보낼 수 있는 방송이라 exported 여도 다른 앱이 흉내 낼 수 없다.
 */
@AndroidEntryPoint
class DailyReminderRescheduleReceiver : BroadcastReceiver() {
    @Inject
    lateinit var coordinator: DailyReminderCoordinator

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in ACTIONS) return
        val pending = goAsync()
        coordinator.resyncAsync(onFinished = pending::finish)
    }

    private companion object {
        val ACTIONS =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
            )
    }
}
