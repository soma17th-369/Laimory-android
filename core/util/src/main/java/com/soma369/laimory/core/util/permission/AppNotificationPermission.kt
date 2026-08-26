package com.soma369.laimory.core.util.permission

import android.Manifest
import android.content.Context
import android.os.Build

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

    /** Android 12 이하는 설치 시점에 허용된 것으로 본다. */
    fun isGranted(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean = sdkInt < Build.VERSION_CODES.TIRAMISU || context.isGranted(Manifest.permission.POST_NOTIFICATIONS)
}
