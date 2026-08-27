package com.soma369.laimory.feature.onboarding.component

import android.content.ActivityNotFoundException
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
import com.soma369.laimory.core.util.permission.NotificationListenerAccess
import com.soma369.laimory.core.util.permission.PhotoPermission
import com.soma369.laimory.feature.onboarding.model.OnboardingPermission

/**
 * 온보딩이 보는 권한 상태와 요청 창구.
 *
 * 상태는 저장하지 않고 볼 때마다 Android 에 묻는다 — 사용자가 시스템 설정에서 언제든 바꿀 수
 * 있어 복제해 두면 곧 어긋난다.
 */
@Stable
internal class OnboardingPermissionState(
    private val granted: Set<OnboardingPermission>,
    private val onRequest: (OnboardingPermission) -> Unit,
) {
    fun isGranted(permission: OnboardingPermission?): Boolean = permission != null && permission in granted

    fun request(permission: OnboardingPermission) = onRequest(permission)
}

/**
 * 화면이 쓸 권한 상태를 만든다.
 *
 * 시스템 화면(알림 접근 설정)에 다녀오면 결과 콜백이 없거나 늦으므로 **ON_RESUME 마다 다시
 * 조회한다.** 요청 다이얼로그의 결과 콜백만 믿으면, 설정에서 켜고 뒤로가기로 돌아온 경우를
 * 놓쳐 화면이 계속 "미허용" 으로 남는다.
 */
@Composable
internal fun rememberOnboardingPermissionState(): OnboardingPermissionState {
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

    val granted =
        remember(refreshKey, context) {
            buildSet {
                if (PhotoPermission.canRead(context)) add(OnboardingPermission.PHOTO)
                if (CalendarPermission.isGranted(context)) add(OnboardingPermission.CALENDAR)
                if (NotificationListenerAccess.isGranted(context)) add(OnboardingPermission.NOTIFICATION_LISTENER)
                if (AppNotificationPermission.isGranted(context)) add(OnboardingPermission.APP_NOTIFICATION)
            }
        }

    return remember(granted) {
        OnboardingPermissionState(granted) { permission ->
            when (permission) {
                OnboardingPermission.PHOTO -> runtimeLauncher.launch(PhotoPermission.required())
                OnboardingPermission.CALENDAR -> runtimeLauncher.launch(CalendarPermission.required())
                OnboardingPermission.LOCATION -> Unit // 3단 순차라 별도 흐름에서 다룬다.
                OnboardingPermission.APP_NOTIFICATION -> {
                    val required = AppNotificationPermission.required()
                    // Android 12 이하는 요청 대상이 아니라 목록이 비어 있다. 빈 배열로 launch 하면
                    // 결과가 즉시 돌아오지만 요청 자체가 성립하지 않으므로 부르지 않는다.
                    if (required.isNotEmpty()) runtimeLauncher.launch(required)
                }

                OnboardingPermission.NOTIFICATION_LISTENER -> {
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
