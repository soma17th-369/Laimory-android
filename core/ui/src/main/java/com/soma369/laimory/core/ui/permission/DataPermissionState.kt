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
    /** 사진을 `일부만 선택` 으로 허용한 상태(Android 14+). 읽기는 되지만 고른 사진에 한한다. */
    private val isPhotoLimited: Boolean,
    /** 이 기기에 알림 접근 설정 화면이 있는지. 없으면 사용자가 켤 방법이 없다. */
    private val hasListenerSettings: Boolean,
    /** Android 11+ 는 `항상 허용` 을 다이얼로그로 주지 않아 앱 설정으로 보내야 한다. */
    private val needsSettingsForBackgroundLocation: Boolean,
    private val onRequest: (DataPermission) -> Unit,
) {
    fun isGranted(permission: DataPermission?): Boolean = permission != null && permission in granted

    /**
     * 소스 하나의 현재 상태.
     *
     * 사진의 `일부 선택` 과 위치의 `전경만` 을 [DataSourceStatus.LIMITED] 로 모은다 — 둘 다
     * 수집은 되지만 범위가 좁은 같은 성격이고, 화면은 "더 넓힐 수 있다" 만 말하면 된다.
     * 알림 접근은 설정 화면이 없는 기기에서 [DataSourceStatus.UNSUPPORTED] 다. 그 경우
     * `허용 안 됨` 이라고 쓰면 사용자가 켤 방법을 찾아 헤매게 된다.
     */
    fun statusOf(permission: DataPermission): DataSourceStatus =
        when (permission) {
            DataPermission.PHOTO ->
                when {
                    !isGranted(permission) -> DataSourceStatus.DENIED
                    isPhotoLimited -> DataSourceStatus.LIMITED
                    else -> DataSourceStatus.GRANTED
                }

            DataPermission.LOCATION ->
                when (locationStep) {
                    LocationPermissionStep.GRANTED -> DataSourceStatus.GRANTED
                    LocationPermissionStep.FOREGROUND -> DataSourceStatus.DENIED
                    // 전경은 열렸고 백그라운드나 활동 인식만 남았다.
                    LocationPermissionStep.BACKGROUND, LocationPermissionStep.ACTIVITY -> DataSourceStatus.LIMITED
                }

            DataPermission.NOTIFICATION_LISTENER ->
                when {
                    isGranted(permission) -> DataSourceStatus.GRANTED
                    !hasListenerSettings -> DataSourceStatus.UNSUPPORTED
                    else -> DataSourceStatus.DENIED
                }

            DataPermission.CALENDAR, DataPermission.APP_NOTIFICATION ->
                if (isGranted(permission)) DataSourceStatus.GRANTED else DataSourceStatus.DENIED
        }

    /** 지금 누를 수 있는 행동 하나. 버튼 문구를 고르는 근거이며 실행은 [request] 가 맡는다. */
    fun actionFor(permission: DataPermission): DataPermissionAction =
        when (statusOf(permission)) {
            DataSourceStatus.GRANTED, DataSourceStatus.UNSUPPORTED -> DataPermissionAction.NONE
            DataSourceStatus.LIMITED ->
                when (permission) {
                    DataPermission.PHOTO -> DataPermissionAction.RESELECT_PHOTOS
                    DataPermission.LOCATION ->
                        if (locationStep == LocationPermissionStep.BACKGROUND && needsSettingsForBackgroundLocation) {
                            DataPermissionAction.APP_SETTINGS
                        } else {
                            DataPermissionAction.REQUEST
                        }

                    else -> DataPermissionAction.REQUEST
                }

            DataSourceStatus.DENIED ->
                if (permission == DataPermission.NOTIFICATION_LISTENER) {
                    DataPermissionAction.LISTENER_SETTINGS
                } else {
                    DataPermissionAction.REQUEST
                }
        }

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

    // 일부 선택과 알림 접근 설정 유무도 같은 refreshKey 로 다시 본다 — 사용자가 시스템 화면에서
    // 사진 선택을 바꾸고 돌아오면 상태 문구가 즉시 따라가야 한다.
    val isPhotoLimited = remember(refreshKey, context) { PhotoPermission.isLimited(context) }
    val hasListenerSettings = remember(refreshKey, context) { NotificationListenerAccess.hasSettings(context) }

    return remember(granted, locationStep, isPhotoLimited, hasListenerSettings) {
        DataPermissionState(
            granted = granted,
            locationStep = locationStep,
            isPhotoLimited = isPhotoLimited,
            hasListenerSettings = hasListenerSettings,
            needsSettingsForBackgroundLocation = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        ) { permission ->
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
