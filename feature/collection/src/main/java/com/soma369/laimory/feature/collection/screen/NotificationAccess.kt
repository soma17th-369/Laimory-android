package com.soma369.laimory.feature.collection.screen

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * 알림 접근(NotificationListener) 권한 판정·설정 진입.
 *
 * 일반 런타임 권한이 아니라 시스템 설정에서 켜야 하므로, `Settings.Secure` 의 활성 리스너 목록으로 판정하고
 * [settingsIntent] 로 설정 화면을 연다. 권한 UX 의 최종 형태는 이번 범위 밖이며 최소 판정만 한다.
 */
internal object NotificationAccess {
    fun isGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, ENABLED_NOTIFICATION_LISTENERS)
        if (enabled.isNullOrEmpty()) return false
        return enabled
            .split(":")
            .mapNotNull { ComponentName.unflattenFromString(it)?.packageName }
            .any { it == context.packageName }
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
}
