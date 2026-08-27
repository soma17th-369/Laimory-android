package com.soma369.laimory.core.ui.permission

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.soma369.laimory.core.util.permission.AppNotificationPermission
import com.soma369.laimory.core.util.permission.CalendarPermission
import com.soma369.laimory.core.util.permission.LocationPermission
import com.soma369.laimory.core.util.permission.NotificationListenerAccess
import com.soma369.laimory.core.util.permission.PhotoPermission

/**
 * 화면이 보는 권한 상태와 요청 창구.
 *
 * 온보딩과 설정이 함께 쓴다. `feature:settings` 는 `feature:onboarding` 을 의존할 수 없으므로
 * (feature 끼리 의존하지 않는다) 공용 자리인 `core:ui` 에 둔다.
 *
 * 상태는 저장하지 않고 볼 때마다 Android 에 묻는다 — 사용자가 시스템 설정에서 언제든 바꿀 수
 * 있어 복제해 두면 곧 어긋난다.
 */
@Stable
class DataPermissionState(
    private val granted: Set<DataPermission>,
    /** 위치는 단계가 있어 따로 본다. 다른 권한은 허용/미허용 둘뿐이다. */
    val locationStep: LocationPermissionStep,
    private val onRequest: (DataPermission) -> Unit,
) {
    fun isGranted(permission: DataPermission?): Boolean = permission != null && permission in granted

    fun request(permission: DataPermission) = onRequest(permission)
}

/**
 * 권한 상태를 만든다.
 *
 * 시스템 화면(알림 접근 설정)에 다녀오면 결과 콜백이 없거나 늦으므로 **ON_RESUME 마다 다시
 * 조회한다.** 요청 다이얼로그의 결과 콜백만 믿으면, 설정에서 켜고 뒤로가기로 돌아온 경우를
 * 놓쳐 화면이 계속 "미허용" 으로 남는다.
 */
@Composable
fun rememberDataPermissionState(): DataPermissionState {
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

    val runtimeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // 결과 맵을 직접 읽지 않고 다시 조회한다. 일부 허용처럼 결과와 실제 상태가 갈리는
            // 경우가 있어, 판정 경로를 하나로 두는 편이 어긋날 여지가 없다.
            refreshKey++
        }
    val settingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshKey++
        }

    val locationStep =
        remember(refreshKey, context) {
            locationPermissionStep(
                // needsForegroundRequest 를 쓰면 안 된다 — 그것은 알림·활동 인식까지 묶어 보므로,
                // 대략 위치만 허용했거나 활동 인식을 거부한 사용자가 FOREGROUND 에 갇힌다.
                hasForeground = LocationPermission.canCollect(context),
                hasBackground = LocationPermission.hasBackground(context),
                hasActivityRecognition = LocationPermission.hasActivityRecognition(context),
            )
        }

    val granted =
        remember(refreshKey, context, locationStep) {
            buildSet {
                if (PhotoPermission.canRead(context)) add(DataPermission.PHOTO)
                if (CalendarPermission.isGranted(context)) add(DataPermission.CALENDAR)
                if (NotificationListenerAccess.isGranted(context)) add(DataPermission.NOTIFICATION_LISTENER)
                if (AppNotificationPermission.isGranted(context)) add(DataPermission.APP_NOTIFICATION)
                if (locationStep == LocationPermissionStep.GRANTED) add(DataPermission.LOCATION)
            }
        }

    return remember(granted, locationStep) {
        DataPermissionState(granted, locationStep) { permission ->
            when (permission) {
                DataPermission.PHOTO -> runtimeLauncher.launch(PhotoPermission.required())
                DataPermission.CALENDAR -> runtimeLauncher.launch(CalendarPermission.required())
                DataPermission.LOCATION ->
                    when (locationStep) {
                        // 위치만 묻는다. 알림·활동 인식은 각자의 자리에서 받는다.
                        LocationPermissionStep.FOREGROUND -> runtimeLauncher.launch(LocationPermission.foreground())
                        // Android 11+ 는 `항상 허용` 을 다이얼로그로 주지 않는다. 앱 설정으로 보내고
                        // 돌아왔을 때 ON_RESUME 재조회가 결과를 반영한다.
                        LocationPermissionStep.BACKGROUND ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                settingsLauncher.launch(appDetailsSettingsIntent(context))
                            } else {
                                runtimeLauncher.launch(arrayOf(LocationPermission.background()))
                            }

                        LocationPermissionStep.ACTIVITY -> runtimeLauncher.launch(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION))
                        LocationPermissionStep.GRANTED -> Unit
                    }
                DataPermission.APP_NOTIFICATION -> {
                    val required = AppNotificationPermission.required()
                    // Android 12 이하는 요청 대상이 아니라 목록이 비어 있다. 빈 배열로 launch 하면
                    // 결과가 즉시 돌아오지만 요청 자체가 성립하지 않으므로 부르지 않는다.
                    if (required.isNotEmpty()) runtimeLauncher.launch(required)
                }

                DataPermission.NOTIFICATION_LISTENER -> {
                    // 알림 접근 화면이 없는 기기가 있다. 확인 없이 열면 ActivityNotFoundException 으로 앱이 죽는다.
                    if (NotificationListenerAccess.hasSettings(context)) {
                        runCatching { settingsLauncher.launch(NotificationListenerAccess.settingsIntent()) }
                            .onFailure { if (it !is ActivityNotFoundException) throw it }
                    }
                }
            }
        }
    }
}

/** 앱 상세 설정. 백그라운드 위치는 Android 11+ 에서 여기서만 켤 수 있다. */
private fun appDetailsSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
