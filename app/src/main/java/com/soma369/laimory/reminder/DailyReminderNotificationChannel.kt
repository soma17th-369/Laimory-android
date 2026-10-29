package com.soma369.laimory.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * 일일 리마인더 채널.
 *
 * 다른 알림과 채널을 나눠, 사용자가 시스템 설정에서 이 종류만 끌 수 있게 한다.
 */
object DailyReminderNotificationChannel {
    const val CHANNEL_ID = "daily_reminder"

    fun create(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "일일 리마인더",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "하루를 기록할 시간에 알려드려요."
            },
        )
    }
}
