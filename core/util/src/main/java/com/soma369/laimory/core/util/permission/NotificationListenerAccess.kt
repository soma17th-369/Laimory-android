package com.soma369.laimory.core.util.permission

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * 알림 **읽기** 접근(NotificationListener) 판정과 설정 진입.
 *
 * 런타임 권한이 아니라 사용자가 시스템 설정에서 켜 주는 접근이라 요청 다이얼로그가 없다.
 * 앱이 할 수 있는 것은 설정 화면을 열어 주고 돌아왔을 때 다시 조회하는 것뿐이다.
 *
 * 판정은 `Settings.Secure` 문자열 파싱 대신 [NotificationManager.isNotificationListenerAccessGranted]
 * 를 쓴다. **패키지가 아니라 컴포넌트 단위**라 같은 앱에 리스너가 둘 이상 생겨도 어느 것이
 * 켜졌는지 구분된다. 지금은 리스너가 하나뿐이라 결과가 같지만, 근거가 되는 값이 다르다.
 */
object NotificationListenerAccess {
    /**
     * 판정 대상 리스너의 정규 클래스 이름.
     *
     * 서비스는 `core:collection` 이 소유하고 이 모듈은 그것을 의존하지 않는다. 이름이 어긋나면
     * 예외 없이 **조용히 항상 미허용**이 되므로, 서비스를 가진 모듈의 테스트가 실제 클래스와 이
     * 값이 같은지 고정한다.
     */
    const val LISTENER_SERVICE_CLASS: String = "com.soma369.laimory.core.collection.notification.LaimoryNotificationListenerService"

    /** 이 앱의 리스너가 켜져 있는지. */
    fun isGranted(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.isNotificationListenerAccessGranted(component(context))
    }

    /** 알림 접근 설정 화면. */
    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    /**
     * 설정 화면을 열 수 있는지.
     *
     * 알림 접근 화면이 없는 기기가 있다. 확인 없이 `startActivity` 하면
     * `ActivityNotFoundException` 으로 앱이 죽으므로 호출부가 먼저 물어야 한다.
     */
    fun hasSettings(context: Context): Boolean = context.packageManager.resolveActivity(settingsIntent(), 0) != null

    private fun component(context: Context): ComponentName = ComponentName(context.packageName, LISTENER_SERVICE_CLASS)
}
