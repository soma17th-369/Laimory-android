package com.soma369.laimory.core.util.permission

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Laimory 가 사용자에게 알림을 **표시**할 권한(`POST_NOTIFICATIONS`).
 *
 * 알림을 **읽는** [NotificationListenerAccess] 와 전혀 다른 권한이다. 이름이 비슷해 섞이기 쉬워
 * 타입으로 갈라 둔다 — 하나는 우리가 보내는 쪽, 하나는 남이 보낸 것을 읽는 쪽이다.
 */
object AppNotificationPermission {
    /**
     * 런타임 요청 목록. Android 12 이하는 요청 대상이 아니라 빈 배열이다.
     *
     * @param sdkInt 판정 기준 SDK. 테스트가 버전별 조합을 고정할 수 있게 주입받는다.
     */
    fun required(sdkInt: Int = Build.VERSION.SDK_INT): Array<String> =
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

    /**
     * 런타임 권한을 받았는지. Android 12 이하는 요청 대상이 아니라 허용된 것으로 본다.
     *
     * **요청할 수 있는지**를 가리는 값이라 알림이 실제로 뜨는지와 다르다. 화면에 "알림이 표시되지
     * 않는다" 를 말하려면 [areNotificationsEnabled] 를 봐야 한다.
     */
    fun isGranted(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean = sdkInt < Build.VERSION_CODES.TIRAMISU || context.isGranted(Manifest.permission.POST_NOTIFICATIONS)

    /**
     * 이 기기가 이 앱의 알림을 실제로 띄울 수 있는지.
     *
     * [isGranted] 와 다른 값이다. Android 12 이하에는 요청할 권한이 아예 없어 [isGranted] 가 늘
     * 참이지만, 사용자는 **어느 버전에서나** 시스템 설정에서 앱 알림을 끌 수 있다. 권한만 보면 그
     * 기기에서 알림이 막힌 것을 영영 알 수 없다. 13+ 의 권한 거부도 여기에 함께 반영된다.
     *
     * 조회할 수 없으면 막히지 않은 것으로 본다 — 확인하지 못한 것을 근거로 "표시되지 않는다" 고
     * 단정하면 멀쩡한 기기에 틀린 안내가 뜬다.
     */
    fun areNotificationsEnabled(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() ?: true

    /**
     * 이 앱의 알림 설정 화면.
     *
     * 런타임 요청은 한 번 거부되면 다시 뜨지 않고, Android 12 이하에는 요청 자체가 없다. 어느
     * 쪽이든 사용자가 직접 켤 수 있는 자리는 여기뿐이다.
     */
    fun settingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
}
