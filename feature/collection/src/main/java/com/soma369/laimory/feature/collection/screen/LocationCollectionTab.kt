package com.soma369.laimory.feature.collection.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.StayPayload
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.feature.collection.state.LocationUiIntent
import com.soma369.laimory.feature.collection.state.LocationUiSideEffect
import com.soma369.laimory.feature.collection.state.LocationUiState
import com.soma369.laimory.feature.collection.viewmodel.LocationCollectionViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.format.DateTimeFormatter

/**
 * 수집 실험실의 "위치" 탭. 토글로 위치 자동 수집을 켜면(foreground) 체류(LOCATION)·이동(MOVEMENT)을 자동 감지·저장한다.
 * 화면 인셋은 컨테이너([CollectionLabRoute])가 소유한다.
 */
@Composable
fun LocationCollectionTab(viewModel: LocationCollectionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LocationCollectionContent(
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun LocationCollectionContent(
    state: LocationUiState,
    onIntent: (LocationUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
    sideEffectFlow: Flow<LocationUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 일괄 삭제 확인 다이얼로그 — LOCATION/MOVEMENT 는 재생성이 어려워 확인 후에만 삭제한다.
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snackbarFlow.collect { message -> snackbarHostState.showSnackbar(message) }
    }
    LaunchedEffect(Unit) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is LocationUiSideEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // location FGS 시작에는 "항상 허용"(백그라운드 위치)이 필요하다. 전경 위치 이후 별도 단계로 요청한다.
    // Android 11+ 는 다이얼로그로 "항상 허용"을 못 받으므로 앱 설정으로 유도하고, 복귀 시 다시 확인해 시작한다.
    val settingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (LocationPermission.hasBackground(context)) {
                onIntent(LocationUiIntent.SetTracking(true))
            } else {
                scope.launch { snackbarHostState.showSnackbar("'항상 허용'이 설정되지 않아 켜지 못했습니다.") }
            }
        }
    val backgroundLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                onIntent(LocationUiIntent.SetTracking(true))
            } else {
                scope.launch { snackbarHostState.showSnackbar("백그라운드 수집은 '항상 허용'이 필요합니다.") }
            }
        }
    val requestBackgroundOrStart: () -> Unit = {
        when {
            LocationPermission.hasBackground(context) -> onIntent(LocationUiIntent.SetTracking(true))
            // Android 11+: 인라인 요청 불가 → 앱 설정으로 유도(복귀 시 settingsLauncher 콜백에서 재확인).
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                scope.launch { snackbarHostState.showSnackbar("위치 권한을 '항상 허용'으로 바꿔 주세요.") }
                settingsLauncher.launch(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            }
            // Android 10: 다이얼로그로 백그라운드 위치 인라인 요청 가능.
            else -> backgroundLauncher.launch(LocationPermission.background())
        }
    }
    // 추적은 위치 권한이 필요하다. 켤 때 전경 권한(위치+알림+활동)을 먼저 받고 이어서 백그라운드 위치를 요청한다.
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (LocationPermission.canCollect(result)) {
                requestBackgroundOrStart()
            } else {
                scope.launch { snackbarHostState.showSnackbar("위치 권한이 필요합니다.") }
            }
        }
    val onToggle: (Boolean) -> Unit = { enabled ->
        when {
            !enabled -> onIntent(LocationUiIntent.SetTracking(false))
            LocationPermission.needsForegroundRequest(context) -> permissionLauncher.launch(LocationPermission.required())
            else -> requestBackgroundOrStart()
        }
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("위치 자동 수집", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = trackingStatusText(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Switch(checked = state.isTracking, onCheckedChange = onToggle)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "스테이징 위치 (${state.stagedItems.size}건)",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(onClick = { showClearConfirm = true }, enabled = state.stagedItems.isNotEmpty()) {
                Text("일괄 삭제")
            }
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("수집된 위치 데이터를 모두 삭제할까요?") },
                text = { Text("체류와 이동 데이터는 다시 만들기 어려울 수 있습니다.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearConfirm = false
                            onIntent(LocationUiIntent.ClearStaged)
                        },
                    ) { Text("삭제") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text("취소") }
                },
            )
        }

        if (state.stagedItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "감지된 위치가 없습니다.\n자동 수집을 켜고 이동/체류하면 쌓입니다.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.stagedItems, key = { it.rawId }) { item ->
                    LocationItem(
                        item = item,
                        onResolveStayAddress = { rawId, latitude, longitude ->
                            onIntent(LocationUiIntent.ResolveStayAddress(rawId, latitude, longitude))
                        },
                        onResolveMovementAddresses = { rawId, start, end ->
                            onIntent(LocationUiIntent.ResolveMovementAddresses(rawId, start, end))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationItem(
    item: SourceItem,
    onResolveStayAddress: (rawId: String, latitude: Double, longitude: Double) -> Unit,
    onResolveMovementAddresses: (rawId: String, start: GeoPoint, end: GeoPoint) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when (val payload = item.payload) {
                is MovementPayload -> {
                    if (payload.start.address.isNullOrBlank() || payload.end.address.isNullOrBlank()) {
                        LaunchedEffect(item.rawId) {
                            onResolveMovementAddresses(item.rawId, payload.start, payload.end)
                        }
                    }
                    Text(
                        text = "이동 · ${payload.transports.label()}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    LocationPoint(label = "출발", point = payload.start)
                    LocationPoint(label = "도착", point = payload.end)
                    Text(
                        text = "거리 ${payload.distanceMeters.toInt()}m · ${item.timeRangeText()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                is StayPayload -> {
                    if (payload.address.isNullOrBlank()) {
                        // LazyColumn에서 실제 구성된 STAY만 요청한다. ViewModel이 rawId 중복 요청을 차단한다.
                        LaunchedEffect(item.rawId) {
                            onResolveStayAddress(item.rawId, payload.latitude, payload.longitude)
                        }
                    }
                    Text(text = "체류 · ${item.durationMinutes()}분", style = MaterialTheme.typography.titleSmall)
                    payload.displayAddress()?.let { address ->
                        Text(text = address, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = payload.coord(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = item.timeRangeText(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun LocationPoint(
    label: String,
    point: GeoPoint,
) {
    Text(
        text = point.displayAddress()?.let { address -> "$label · $address" } ?: label,
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text = point.coord(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

private fun MovementPayload.Transport.label(): String =
    when (this) {
        MovementPayload.Transport.WALKING -> "도보"
        MovementPayload.Transport.RUNNING -> "뛰기"
        MovementPayload.Transport.ON_BICYCLE -> "자전거"
        MovementPayload.Transport.IN_VEHICLE -> "차량"
        MovementPayload.Transport.UNKNOWN -> "알 수 없음"
    }

internal fun GeoPoint.coord(): String = "%.5f, %.5f".format(latitude, longitude)

internal fun StayPayload.coord(): String = "%.5f, %.5f".format(latitude, longitude)

internal fun GeoPoint.displayAddress(): String? = address?.takeIf(String::isNotBlank)

internal fun StayPayload.displayAddress(): String? = address?.takeIf(String::isNotBlank)

/** 체류 시간(분) — startAt/endAt 에서 파생(payload 미저장). */
private fun SourceItem.durationMinutes(): Long {
    val end = endAt ?: return 0
    return Duration.between(startAt, end).toMinutes()
}

private val timeFormatter = DateTimeFormatter.ofPattern("M/d(E) HH:mm")

private fun SourceItem.timeRangeText(): String {
    val start = startAt.atZone(timeZoneId).format(timeFormatter)
    val end = endAt?.atZone(timeZoneId)?.format(timeFormatter)
    return if (end != null) "$start ~ $end" else start
}

/** 토글 카드 부제 — 추적 중이면 라이브 상태(체류 중 N분 / 이동 중 / 위치 확인 중), 아니면 꺼짐. */
private fun trackingStatusText(state: LocationUiState): String =
    when {
        !state.isTracking -> "꺼짐"
        else ->
            when (val status = state.trackingStatus) {
                is LocationTrackingStatus.Dwelling -> {
                    val minutes = (status.nowMillis - status.sinceMillis) / 60_000L
                    "체류 중 · ${minutes}분 (%.5f, %.5f)".format(status.latitude, status.longitude)
                }
                LocationTrackingStatus.Moving -> "이동 중"
                null -> "위치 확인 중…"
            }
    }
