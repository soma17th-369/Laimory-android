package com.soma369.laimory.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * 초안 완료 알림 채널.
 *
 * 알림은 서버가 보낸 `notification` payload 를 FCM SDK 가 이 채널로 띄운다. 앱은 채널을 만들고
 * 다 본 알림을 지우는 것만 한다.
 */
object DraftCompletionNotificationChannel {
    const val CHANNEL_ID = "timeline_draft_completion"

    /**
     * 이 채널에 남아 있는 알림을 모두 지운다.
     *
     * 알림을 누르지 않고 앱을 직접 열어 결과를 확인한 경우, 알림만 남아 이미 끝난 일을 계속
     * 알린다. 다른 채널의 알림은 건드리지 않는다.
     */
    fun dismissAll(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        notificationManager.activeNotifications
            .filter { posted -> posted.notification.channelId == CHANNEL_ID }
            .forEach { posted -> notificationManager.cancel(posted.tag, posted.id) }
    }

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
