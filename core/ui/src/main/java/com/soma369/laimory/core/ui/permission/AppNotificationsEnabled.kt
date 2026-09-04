package com.soma369.laimory.core.ui.permission

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.soma369.laimory.core.util.permission.AppNotificationPermission

/**
 * 이 기기가 앱 알림을 띄울 수 있는지.
 *
 * 런타임 권한([DataPermission.APP_NOTIFICATION])과 다른 값이다. 그쪽은 **요청할 수 있는지**를
 * 가리므로 Android 12 이하에서는 늘 허용으로 나오지만, 사용자는 어느 버전에서나 시스템 설정에서
 * 앱 알림을 끌 수 있다.
 *
 * 시스템 설정에 다녀오면 결과 콜백이 없으므로 **ON_RESUME 마다 다시 묻는다.**
 */
@Composable
fun rememberAppNotificationsEnabled(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) refreshKey++
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(refreshKey, context) { AppNotificationPermission.areNotificationsEnabled(context) }
}

/**
 * 이 앱의 알림 설정 화면을 연다.
 *
 * 알림 설정 화면이 없는 기기가 있어 앱 정보 화면으로 떨어뜨린다. 확인 없이 열면
 * `ActivityNotFoundException` 으로 앱이 죽는다.
 */
fun openAppNotificationSettings(context: Context) {
    runCatching { context.startActivity(AppNotificationPermission.settingsIntent(context)) }
        .onFailure { error ->
            if (error !is ActivityNotFoundException) throw error
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        }
}
