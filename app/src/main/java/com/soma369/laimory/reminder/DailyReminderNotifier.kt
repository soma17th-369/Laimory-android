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
    ) {
        private val picker = DailyReminderCopyPicker()

        /** 문구 후보 수. 리소스 배열 길이를 그대로 쓴다. */
        private val generalCount: Int get() = context.resources.getStringArray(R.array.daily_reminder_general_titles).size
        private val completedCount: Int get() = context.resources.getStringArray(R.array.daily_reminder_completed_titles).size

        internal fun pickGeneral(lastIndex: Int?): DailyReminderCopy.General = picker.pickGeneral(generalCount, lastIndex)

        internal fun pickCompleted(lastIndex: Int?): DailyReminderCopy.Completed = picker.pickCompleted(completedCount, lastIndex)

        /**
         * 알림을 띄운다. 기기가 알림을 표시할 수 없으면 아무것도 하지 않고 `false`.
         *
         * @param recordDate 완료 문구일 때 누르면 열 기록 날짜.
         */
        internal fun notify(
            copy: DailyReminderCopy,
            recordDate: LocalDate,
        ): Boolean {
            if (!canPost()) return false
            val resources = context.resources
            val (title, body) =
                when (copy) {
                    is DailyReminderCopy.General ->
                        resources.getStringArray(R.array.daily_reminder_general_titles)[copy.index] to
                            resources.getStringArray(R.array.daily_reminder_general_bodies)[copy.index]
                    is DailyReminderCopy.Completed ->
                        resources.getStringArray(R.array.daily_reminder_completed_titles)[copy.index] to
                            resources.getStringArray(R.array.daily_reminder_completed_bodies)[copy.index]
                }
            val destinationDate = recordDate.takeIf { copy is DailyReminderCopy.Completed }
            val contentIntent =
                PendingIntent.getActivity(
                    context,
                    REQUEST_CODE_OPEN,
                    DailyReminderTap.put(
                        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        destinationDate,
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

        private fun canPost(): Boolean {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
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
