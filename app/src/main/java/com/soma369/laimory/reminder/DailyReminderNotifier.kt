package com.soma369.laimory.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.soma369.laimory.MainActivity
import com.soma369.laimory.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import com.soma369.laimory.core.util.R as UtilR

/** 리마인더 알림을 그려 띄운다. 무엇을 띄울지는 [DailyReminderDelivery] 가 정한다. */
@Singleton
class DailyReminderNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DailyReminderNotifications {
        private val picker = DailyReminderCopyPicker()

        /** 문구 후보 수. 리소스 배열 길이를 그대로 쓴다. */
        private val generalCount: Int get() = context.resources.getStringArray(R.array.daily_reminder_general_titles).size
        private val completedCount: Int get() = context.resources.getStringArray(R.array.daily_reminder_completed_titles).size

        override fun pickGeneral(lastIndex: Int?): DailyReminderCopy.General = picker.pickGeneral(generalCount, lastIndex)

        override fun pickCompleted(lastIndex: Int?): DailyReminderCopy.Completed = picker.pickCompleted(completedCount, lastIndex)

        override fun titleOf(copy: DailyReminderCopy): String =
            when (copy) {
                is DailyReminderCopy.General -> context.resources.getStringArray(R.array.daily_reminder_general_titles)[copy.index]
                is DailyReminderCopy.Completed -> context.resources.getStringArray(R.array.daily_reminder_completed_titles)[copy.index]
            }

        override fun notify(
            copy: DailyReminderCopy,
            recordDate: LocalDate,
        ): Boolean {
            if (!canPost()) return false
            val resources = context.resources
            val title = titleOf(copy)
            val body =
                when (copy) {
                    is DailyReminderCopy.General -> resources.getStringArray(R.array.daily_reminder_general_bodies)[copy.index]
                    is DailyReminderCopy.Completed -> resources.getStringArray(R.array.daily_reminder_completed_bodies)[copy.index]
                }
            val destinationDate = recordDate.takeIf { copy is DailyReminderCopy.Completed }
            val contentIntent =
                PendingIntent.getActivity(
                    context,
                    REQUEST_CODE_OPEN,
                    DailyReminderTap.put(
                        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        destinationDate,
                        copy.toAnalyticsCopy(title),
                    ),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            val notification =
                NotificationCompat
                    .Builder(context, DailyReminderNotificationChannel.CHANNEL_ID)
                    .setSmallIcon(UtilR.drawable.ico_notification_small)
                    .setColor(ContextCompat.getColor(context, R.color.notification_accent))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .build()
            return try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
                true
            } catch (_: SecurityException) {
                false
            }
        }

        /**
         * 지금 띄우면 사용자에게 보이는지.
         *
         * 앱 전체 알림이 켜져 있어도 **이 채널만** 끌 수 있다. 그때 `notify` 는 예외 없이 끝나지만 시스템이 보이지 않게
         * 버리므로, 채널 중요도까지 본다 — 보이지 않은 알림을 띄웠다고 기록하면 다음 날 문구 선택과 표시 집계가 어긋난다.
         */
        private fun canPost(): Boolean {
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) return false
            val channel = notificationManager.getNotificationChannelCompat(DailyReminderNotificationChannel.CHANNEL_ID)
            if (channel?.importance == NotificationManagerCompat.IMPORTANCE_NONE) return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }

        private companion object {
            /** 하루에 하나라 같은 자리를 덮어쓴다. 어제 알림이 남아 있으면 오늘 것으로 바뀐다. */
            const val NOTIFICATION_ID = 2100
            const val REQUEST_CODE_OPEN = 2100
        }
    }
