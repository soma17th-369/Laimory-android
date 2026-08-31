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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.soma369.laimory.core.util.permission.AppNotificationPermission
import com.soma369.laimory.core.util.permission.CalendarPermission
import com.soma369.laimory.core.util.permission.LocationPermission
import com.soma369.laimory.core.util.permission.NotificationListenerAccess
import com.soma369.laimory.core.util.permission.PhotoPermission
import com.soma369.laimory.core.util.permission.isPermanentlyDenied

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
    /** Health Connect 를 쓸 수 있는 기기인지. 미설치·업데이트 필요면 요청 자체가 성립하지 않는다. */
    private val isHealthAvailable: Boolean = false,
    /**
     * 요청을 보냈지만 시스템이 다이얼로그를 띄우지 않은 권한.
     *
     * 두 번 거부하면 Android 는 요청을 조용히 삼킨다. 이때도 `허용하기` 를 보여 주면 눌러도
     * 아무 일이 없는 버튼이 되므로 설정으로 길을 바꾼다.
     */
    private val blocked: Set<DataPermission> = emptySet(),
    /** 앱이 켜고 끌 수 없는 것을 사용자가 직접 바꾸러 가는 창구. */
    private val onOpenSettings: (DataPermission) -> Unit = {},
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

            // Health Connect 가 없는 기기에서는 허용할 방법이 없다. `허용 안 됨` 으로 쓰면
            // 사용자가 열 수 없는 화면을 찾게 된다.
            DataPermission.HEALTH ->
                when {
                    !isHealthAvailable -> DataSourceStatus.UNSUPPORTED
                    isGranted(permission) -> DataSourceStatus.GRANTED
                    else -> DataSourceStatus.DENIED
                }

            DataPermission.CALENDAR, DataPermission.APP_NOTIFICATION ->
                if (isGranted(permission)) DataSourceStatus.GRANTED else DataSourceStatus.DENIED
        }

    /**
     * 지금 누를 수 있는 행동 하나. 버튼 문구를 고르는 근거이며 실행은 [act] 가 맡는다.
     *
     * 이미 허용된 소스도 막다른 길로 두지 않는다 — 사용자가 설정 화면에 들어오는 이유의 절반은
     * **끄거나 좁히려는 것**이다. 앱에서 바로 회수하면 프로세스가 종료되므로 시스템 설정에서
     * 안전하게 바꾸도록 보낸다.
     */
    fun actionFor(permission: DataPermission): DataPermissionAction =
        when (statusOf(permission)) {
            // 열 방법이 없는 기기에서만 아무것도 주지 않는다.
            DataSourceStatus.UNSUPPORTED -> DataPermissionAction.NONE
            DataSourceStatus.GRANTED ->
                when {
                    // 헬스는 런타임 권한이 아니라 Health Connect 가 관리한다.
                    permission == DataPermission.HEALTH -> DataPermissionAction.HEALTH_SETTINGS
                    // 알림 읽기는 특수 접근이라 전용 설정으로 보낸다.
                    permission == DataPermission.NOTIFICATION_LISTENER -> DataPermissionAction.LISTENER_SETTINGS
                    else -> DataPermissionAction.APP_SETTINGS
                }
            DataSourceStatus.LIMITED ->
                when {
                    // 다시 고르는 것 자체가 넓히는 길이라, 시스템이 매번 선택 화면을 띄운다.
                    permission == DataPermission.PHOTO -> DataPermissionAction.RESELECT_PHOTOS
                    permission == DataPermission.LOCATION &&
                        locationStep == LocationPermissionStep.BACKGROUND &&
                        needsSettingsForBackgroundLocation -> DataPermissionAction.APP_SETTINGS
                    // 남은 한 단계를 두 번 거부한 경우다. 위치는 `일부 허용` 도 LIMITED 라
                    // DENIED 분기에 닿지 않으므로, 막힌 판정을 여기서도 똑같이 봐야 한다.
                    permission in blocked -> DataPermissionAction.APP_SETTINGS
                    else -> DataPermissionAction.REQUEST
                }

            DataSourceStatus.DENIED ->
                when {
                    permission == DataPermission.NOTIFICATION_LISTENER -> DataPermissionAction.LISTENER_SETTINGS
                    // 시스템이 더 이상 묻지 않으므로 다이얼로그를 기다릴 수 없다.
                    permission in blocked -> DataPermissionAction.APP_SETTINGS
                    else -> DataPermissionAction.REQUEST
                }
        }

    fun request(permission: DataPermission) = onRequest(permission)

    /**
     * 지금 상태에 맞는 행동을 실행한다.
     *
     * 화면이 [actionFor] 로 분기해 직접 호출부를 고르면, 새 상태가 생길 때마다 화면도 같이
     * 고쳐야 한다. 어느 창구로 갈지는 여기서 정한다.
     */
    fun act(permission: DataPermission) {
        when (actionFor(permission)) {
            DataPermissionAction.NONE -> Unit
            DataPermissionAction.APP_SETTINGS, DataPermissionAction.HEALTH_SETTINGS -> onOpenSettings(permission)
            else -> onRequest(permission)
        }
    }
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

    // 어느 소스를 요청했는지 알아야 결과를 그 소스에 귀속시킬 수 있다.
    var pending by remember { mutableStateOf<DataPermission?>(null) }
    var blocked by remember { mutableStateOf(emptySet<DataPermission>()) }
    val runtimeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            // 결과 맵을 직접 읽지 않고 다시 조회한다. 일부 허용처럼 결과와 실제 상태가 갈리는
            // 경우가 있어, 판정 경로를 하나로 두는 편이 어긋날 여지가 없다.
            val requested = pending
            if (requested != null) {
                val keys = result.keys.toTypedArray()
                // 요청 **직후** 의 rationale=false 는 "물어봤는데 다이얼로그가 안 떴다" 는 뜻이다.
                blocked =
                    if (keys.isNotEmpty() && context.isPermanentlyDenied(keys)) {
                        blocked + requested
                    } else {
                        blocked - requested
                    }
            }
            pending = null
            refreshKey++
        }
    val settingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshKey++
        }
    // Health Connect 는 자체 권한 화면을 연다. 결과 집합을 읽지 않고 다시 조회하는 것은
    // 다른 권한과 같은 이유다 — 판정 경로를 하나로 둔다.
    val healthLauncher =
        rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
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

    // Health Connect 는 시스템이 아니라 그 앱에 물어야 하고 답이 suspend 로 온다. 조회가
    // 일시적으로 실패했을 때 `거부` 로 낮추지 않는다 — 허용해 둔 사용자에게 틀린 문구가 뜬다.
    val isHealthAvailable =
        remember(refreshKey, context) { HealthDataSource.isEnabled && HealthDataSource.isAvailable(context) }
    var isHealthGranted by remember { mutableStateOf(false) }
    LaunchedEffect(refreshKey, isHealthAvailable) {
        isHealthGranted =
            isHealthAvailable && runCatching { HealthDataSource.isGranted(context) }.getOrDefault(isHealthGranted)
    }

    val granted =
        remember(refreshKey, context, locationStep, isHealthGranted) {
            buildSet {
                if (PhotoPermission.canRead(context)) add(DataPermission.PHOTO)
                if (CalendarPermission.isGranted(context)) add(DataPermission.CALENDAR)
                if (NotificationListenerAccess.isGranted(context)) add(DataPermission.NOTIFICATION_LISTENER)
                if (AppNotificationPermission.isGranted(context)) add(DataPermission.APP_NOTIFICATION)
                if (locationStep == LocationPermissionStep.GRANTED) add(DataPermission.LOCATION)
                if (isHealthGranted) add(DataPermission.HEALTH)
            }
        }

    // 일부 선택과 알림 접근 설정 유무도 같은 refreshKey 로 다시 본다 — 사용자가 시스템 화면에서
    // 사진 선택을 바꾸고 돌아오면 상태 문구가 즉시 따라가야 한다.
    val isPhotoLimited = remember(refreshKey, context) { PhotoPermission.isLimited(context) }
    val hasListenerSettings = remember(refreshKey, context) { NotificationListenerAccess.hasSettings(context) }

    return remember(granted, locationStep, isPhotoLimited, hasListenerSettings, isHealthAvailable, blocked) {
        DataPermissionState(
            granted = granted,
            locationStep = locationStep,
            isPhotoLimited = isPhotoLimited,
            hasListenerSettings = hasListenerSettings,
            needsSettingsForBackgroundLocation = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
            isHealthAvailable = isHealthAvailable,
            blocked = blocked,
            onOpenSettings = { permission ->
                // 헬스 권한은 앱 설정에 나오지 않는다. Health Connect 앱이 자기 권한 화면을 갖는다.
                val intent =
                    if (permission == DataPermission.HEALTH) {
                        // 액션 문자열을 직접 쓰지 않는다 — Android 14+ 는 플랫폼 액션으로 이름이
                        // 바뀌었고, 라이브러리 상수만 SDK 에 맞는 쪽을 골라 준다.
                        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                    } else {
                        appDetailsSettingsIntent(context)
                    }
                runCatching { settingsLauncher.launch(intent) }
                    .onFailure { if (it !is ActivityNotFoundException) throw it }
            },
        ) { permission ->
            when (permission) {
                DataPermission.PHOTO -> {
                    pending = permission
                    runtimeLauncher.launch(PhotoPermission.required())
                }

                DataPermission.CALENDAR -> {
                    pending = permission
                    runtimeLauncher.launch(CalendarPermission.required())
                }
                DataPermission.LOCATION ->
                    when (locationStep) {
                        // 위치만 묻는다. 알림·활동 인식은 각자의 자리에서 받는다.
                        LocationPermissionStep.FOREGROUND -> {
                            pending = permission
                            runtimeLauncher.launch(LocationPermission.foreground())
                        }
                        // Android 11+ 는 `항상 허용` 을 다이얼로그로 주지 않는다. 앱 설정으로 보내고
                        // 돌아왔을 때 ON_RESUME 재조회가 결과를 반영한다.
                        LocationPermissionStep.BACKGROUND ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                settingsLauncher.launch(appDetailsSettingsIntent(context))
                            } else {
                                // 다이얼로그를 띄우는 길에는 반드시 pending 을 남긴다 — 남기지
                                // 않으면 막혔다는 사실을 결과 콜백이 어디에도 기록하지 못한다.
                                pending = permission
                                runtimeLauncher.launch(arrayOf(LocationPermission.background()))
                            }

                        LocationPermissionStep.ACTIVITY -> {
                            pending = permission
                            runtimeLauncher.launch(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION))
                        }
                        LocationPermissionStep.GRANTED -> Unit
                    }
                DataPermission.APP_NOTIFICATION -> {
                    val required = AppNotificationPermission.required()
                    // Android 12 이하는 요청 대상이 아니라 목록이 비어 있다. 빈 배열로 launch 하면
                    // 결과가 즉시 돌아오지만 요청 자체가 성립하지 않으므로 부르지 않는다.
                    if (required.isNotEmpty()) runtimeLauncher.launch(required)
                }

                // 쓸 수 없는 기기에서 요청 화면을 열면 Health Connect 가 없다는 오류로 끝난다.
                DataPermission.HEALTH -> if (isHealthAvailable) healthLauncher.launch(HealthDataSource.required)

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
