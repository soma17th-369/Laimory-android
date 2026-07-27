package com.soma369.laimory.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object DraftCompletionNotificationChannel {
    const val CHANNEL_ID = "timeline_draft_completion"

    fun create(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "타임라인 생성 알림",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "타임라인 초안 생성 완료 또는 실패를 알려드려요."
            },
        )
    }
}
