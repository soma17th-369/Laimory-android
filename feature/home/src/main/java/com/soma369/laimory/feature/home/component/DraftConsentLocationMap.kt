package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.state.ConsentLocationMarker
import com.soma369.laimory.feature.home.state.ConsentMapBounds
import com.soma369.laimory.feature.home.state.toBounds
import kotlinx.coroutines.delay

/**
 * 위치 상세 지도. 전송되는 좌표가 어디인지 보여준다.
 *
 * 핀에는 목록과 같은 번호를 찍는다 — 주소가 해석되지 않은 항목이 적지 않아 제목만으로는 어느
 * 핀이 어느 줄인지 대조할 수 없다.
 *
 * 체류와 이동은 색으로 구분하고, 이동은 시작을 채운 핀·도착을 테두리 핀으로 나눈다. 두 점을
 * 잇는 선은 그리지 않는다 — 직선은 실제 경로가 아니고 누적 거리와도 맞지 않아 오해만 만든다.
 *
 * 목록 밖 고정 영역에 정사각형으로 둔다. 목록 안에 넣으면 스크롤로 사라지는 데다 지도 드래그와
 * 목록 스크롤이 같은 세로 제스처를 두고 다툰다.
 *
 * 포함·미포함은 **핀 색**으로 나눈다. 제외한 항목도 흐리게 남겨 두는 이유는 지도에서 다시
 * 켤 수 있어야 하기 때문이다 — 감추면 끄기만 되고 켜기는 목록에서만 되는 한쪽 문이 된다.
 *
 * 토글은 핀을 바로 누르는 대신 **말풍선을 거친다.** 지도를 움직이다 핀이 눌려 실수로 꺼지는
 * 일을 막고, 어떤 장소·언제인지 보고 결정하게 한다.
 *
 * [renderAllowed] 가 false 면 `GoogleMap` 을 **composition 에 넣지 않는다.** 지도를 그리는 순간
 * 카메라 영역이 Google 로 나가므로, 동의 전이나 API 키가 없을 때는 같은 크기의 대체 영역만 둔다.
 */
@Composable
internal fun DraftConsentLocationMap(
    markers: List<ConsentLocationMarker>,
    excludedRawIds: Set<String>,
    renderAllowed: Boolean,
    onToggleMarker: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // 정사각형이되 화면 높이의 일부를 넘지 않게 자른다. 가로 화면에서는 폭이 세로보다 훨씬
        // 커서 1:1 을 그대로 쓰면 지도가 화면을 통째로 먹고 아래 목록이 사라진다.
        val size = minOf(maxWidth, screenHeight * MAX_SCREEN_HEIGHT_FRACTION)
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(size),
            shape = RoundedCornerShape(MAP_CORNER_RADIUS),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            // 키가 있어도 값이 틀렸거나 앱 제한이 안 맞거나 네트워크가 막히면 SDK 가 조용히
            // 빈 지도를 그린다. 그 상태를 감지해 같은 자리 대체 안내로 넘긴다.
            var loadFailed by remember { mutableStateOf(false) }
            when {
                markers.isEmpty() -> MapPlaceholder("전송할 위치가 없어요.")
                !renderAllowed || loadFailed ->
                    MapPlaceholder("지도를 표시할 수 없어요. 아래 목록에서 장소와 시간을 확인할 수 있어요.")

                else ->
                    LocationMarkerMap(
                        markers = markers,
                        excludedRawIds = excludedRawIds,
                        onToggleMarker = onToggleMarker,
                        onLoadFailed = { loadFailed = true },
                    )
            }
        }
    }
}

@Composable
private fun LocationMarkerMap(
    markers: List<ConsentLocationMarker>,
    excludedRawIds: Set<String>,
    onToggleMarker: (String) -> Unit,
    onLoadFailed: () -> Unit,
) {
    /*
     * 지도 SDK 는 인증 실패를 콜백으로 알려 주지 않는다 — 키가 틀리거나 앱 제한이 안 맞으면
     * 로그만 남기고 빈 지도를 그린다. 타일이 한 번이라도 그려졌는지(`onMapLoaded`)로 대신 본다.
     * 제한 시간 안에 소식이 없으면 실패로 보고 대체 안내로 넘긴다.
     *
     * 한 번 성공한 뒤에는 되돌리지 않는다. 이후 네트워크가 끊겨도 이미 그려진 지도를 지우면
     * 사용자가 보던 화면이 사라진다. 반대로 실패로 넘어간 뒤 네트워크가 돌아와도 다시 붙지
     * 않는다 — 화면을 다시 열면 재시도된다.
     */
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(MAP_LOAD_TIMEOUT_MILLIS)
        if (!loaded) onLoadFailed()
    }

    // 지도는 목록 밖 고정 영역이라 스크롤로 폐기되지 않는다. 카메라는 최초 1회만 맞추고,
    // 항목을 켜고 끌 때마다 되돌리지 않는다 — 사용자가 보던 영역이 사라진다.
    // 그래서 remember 에 key 를 두지 않는다(마커가 줄어도 다시 계산하지 않는다).
    val initial = remember { markers.toBounds()?.toInitialCameraPosition() }
    val cameraPositionState = rememberCameraPositionState { initial?.let { position = it } }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        onMapLoaded = { loaded = true },
        uiSettings =
            MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
            ),
    ) {
        markers.forEach { marker ->
            // 마커 상태는 재구성마다 새로 만들면 안 된다(선택·정보창 상태가 날아간다).
            // 좌표가 그대로면 같은 인스턴스를 유지한다.
            val markerState =
                remember(marker.key, marker.latitude, marker.longitude) {
                    MarkerState(position = LatLng(marker.latitude, marker.longitude))
                }
            NumberedMarker(
                marker = marker,
                state = markerState,
                included = marker.sourceRawId !in excludedRawIds,
                onInfoWindowClick = { onToggleMarker(marker.sourceRawId) },
            )
        }
    }
}

/**
 * 번호를 찍은 핀.
 *
 * 기본 [Marker] 는 글자를 넣을 수 없어 Compose 로 직접 그린다. `keys` 는 그린 결과를 다시 쓸지
 * 판단하는 값이라 **핀 모양을 바꾸는 입력을 모두** 넣어야 한다 — 빠뜨리면 번호가 바뀌어도
 * 이전 그림이 그대로 남는다.
 */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
@GoogleMapComposable
private fun NumberedMarker(
    marker: ConsentLocationMarker,
    state: MarkerState,
    included: Boolean,
    onInfoWindowClick: () -> Unit,
) {
    val fill = if (included) marker.kind.fillColor() else excludedFillColor()
    val onFill = if (included) marker.kind.contentColor() else excludedContentColor()
    val border = if (included) marker.kind.borderColor() else excludedContentColor()
    MarkerComposable(
        // 핀 그림을 바꾸는 입력을 모두 넣는다. 빠뜨리면 상태가 바뀌어도 이전 그림이 남는다.
        keys = arrayOf(marker.key, marker.order, marker.kind, included),
        state = state,
        title = marker.title,
        // 말풍선을 눌러 바꾸는 값이라 현재 상태를 함께 적는다.
        snippet = listOfNotNull(marker.snippet, if (included) "포함" else "미포함").joinToString(" · "),
        onInfoWindowClick = { onInfoWindowClick() },
    ) {
        Box(
            modifier =
                Modifier
                    .size(MARKER_SIZE)
                    .background(color = fill, shape = CircleShape)
                    .border(width = MARKER_BORDER_WIDTH, color = border, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = marker.order.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = onFill,
            )
        }
    }
}

/** 미포함 핀. 종류를 더 나누지 않는다 — 어차피 전송되지 않아 체류·이동 구분이 의미 없다. */
@Composable
private fun excludedFillColor(): Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = EXCLUDED_MARKER_ALPHA)

@Composable
private fun excludedContentColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = EXCLUDED_MARKER_ALPHA)

/** 체류는 채운 primary, 이동 시작은 채운 tertiary, 이동 도착은 속을 비워 방향을 읽게 한다. */
@Composable
private fun ConsentLocationMarker.Kind.fillColor(): Color =
    when (this) {
        ConsentLocationMarker.Kind.STAY -> MaterialTheme.colorScheme.primary
        ConsentLocationMarker.Kind.MOVEMENT_START -> MaterialTheme.colorScheme.tertiary
        ConsentLocationMarker.Kind.MOVEMENT_END -> MaterialTheme.colorScheme.surface
    }

@Composable
private fun ConsentLocationMarker.Kind.contentColor(): Color =
    when (this) {
        ConsentLocationMarker.Kind.STAY -> MaterialTheme.colorScheme.onPrimary
        ConsentLocationMarker.Kind.MOVEMENT_START -> MaterialTheme.colorScheme.onTertiary
        ConsentLocationMarker.Kind.MOVEMENT_END -> MaterialTheme.colorScheme.tertiary
    }

@Composable
private fun ConsentLocationMarker.Kind.borderColor(): Color =
    when (this) {
        ConsentLocationMarker.Kind.STAY -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.tertiary
    }

/**
 * 지도가 없을 때 같은 자리를 차지하는 안내.
 *
 * 지도 유무로 아래 목록과 Switch 의 위치가 달라지지 않게 크기를 유지한다.
 */
@Composable
private fun MapPlaceholder(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(Spacing.extraLarge),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 마커 전체가 들어오는 최초 카메라.
 *
 * `LatLngBounds` 로 맞추려면 지도 크기가 측정된 뒤여야 해서 첫 프레임에 예외가 날 수 있다.
 * 중심과 배율로 잡으면 측정 전에도 안전하다.
 */
private fun ConsentMapBounds.toInitialCameraPosition(): CameraPosition {
    val southWest = LatLng(southLatitude, westLongitude)
    val northEast = LatLng(northLatitude, eastLongitude)
    val center = LatLngBounds(southWest, northEast).center
    return CameraPosition.fromLatLngZoom(center, zoomFor(spanDegrees()))
}

private fun ConsentMapBounds.spanDegrees(): Double = maxOf(northLatitude - southLatitude, eastLongitude - westLongitude)

/**
 * 영역 폭을 배율로 옮긴다. 세계 지도가 배율 0에서 360도를 덮는 관계(`360 / 2^zoom`)를 뒤집은 값이다.
 * 마커가 가장자리에 붙지 않도록 한 단계 낮춘 뒤 지도 배율 범위로 자른다.
 */
private fun zoomFor(spanDegrees: Double): Float {
    val zoom = (kotlin.math.ln(WORLD_SPAN_DEGREES / spanDegrees) / kotlin.math.ln(2.0)) - ZOOM_MARGIN_STEPS
    return zoom.toFloat().coerceIn(MIN_ZOOM, MAX_ZOOM)
}

private const val WORLD_SPAN_DEGREES = 360.0
private const val ZOOM_MARGIN_STEPS = 1.0
private const val MIN_ZOOM = 2f
private const val MAX_ZOOM = 17f
private val MAP_CORNER_RADIUS = 16.dp

/**
 * 지도가 차지할 수 있는 화면 높이의 최대 비율.
 *
 * dp 는 물리 크기를 맞춰 주지만 화면이 몇 dp 인지는 기기마다 다르다. 폭으로만 정사각형을 만들면
 * 가로 화면·태블릿에서 목록이 밀려나므로 화면 높이로 한 번 더 자른다.
 */
private const val MAX_SCREEN_HEIGHT_FRACTION = 0.45f

/**
 * 첫 타일이 그려지기를 기다리는 시간.
 *
 * 인증 실패는 즉시 판가름 나지만 느린 네트워크도 있어 넉넉히 준다. 이 시간을 넘기면 사용자가
 * 빈 지도를 계속 보는 편보다 목록을 쓰라고 안내하는 편이 낫다.
 */
private const val MAP_LOAD_TIMEOUT_MILLIS = 8_000L
private val MARKER_SIZE = 28.dp
private val MARKER_BORDER_WIDTH = 2.dp

/** 미포함 핀의 흐림 정도. 어디였는지는 보이되 포함된 핀과 확실히 갈리는 값. */
private const val EXCLUDED_MARKER_ALPHA = 0.55f
