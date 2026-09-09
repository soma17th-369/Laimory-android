package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.StayPayload
import java.time.ZoneId

/**
 * 위치 상세 지도에 찍는 마커 한 개.
 *
 * 알림·사진·일정이 함께 쓰는 [DraftConsentDetailItem] 과 분리한 위치 전용 모델이다 —
 * 좌표를 공용 모델에 nullable 로 얹으면 좌표를 쓰지 않는 유형까지 그 필드를 들고 다닌다.
 *
 * 체류와 이동을 구분하지 않는다. 지도는 "전송되는 좌표가 어디인지" 만 보여주고, 의미 구분은
 * 아래 목록이 맡는다.
 */
@Immutable
data class ConsentLocationMarker(
    /**
     * 마커 식별자.
     *
     * `MOVEMENT` 는 시작·종료 두 마커가 같은 `rawId` 에서 나오므로 접미사로 구분한다.
     * 전송 포함 여부는 `rawId` 단위라 [sourceRawId] 를 따로 들고 있는다.
     */
    val key: String,
    val sourceRawId: String,
    /**
     * 지도와 목록을 잇는 번호. 시간순 1부터다.
     *
     * 주소가 없는 항목이 20% 넘게 나오는 상황이라 제목만으로는 어느 핀이 어느 줄인지 알 수 없다.
     * 같은 번호를 핀과 목록 카드에 함께 찍어 대조할 수 있게 한다.
     */
    val order: Int,
    val kind: Kind,
    val latitude: Double,
    val longitude: Double,
    /** 주소 요약. 위경도 숫자는 사용자에게 직접 보이지 않는다. */
    val title: String,
    /** 시각 등 보조 설명. */
    val snippet: String?,
) {
    /** 핀 모양을 가르는 구분. 이동은 시작과 도착이 한 항목의 양 끝이라 따로 본다. */
    enum class Kind { STAY, MOVEMENT_START, MOVEMENT_END }
}

/**
 * 표시 시점에 해석한 주소를 스냅샷 위에 덧입힐 때 쓰는 키.
 *
 * 전송 스냅샷의 payload 는 건드리지 않고 화면 모델만 이 맵을 본다. 지도 마커와 아래 목록이 같은
 * 키를 보므로 두 곳의 주소가 어긋날 수 없다.
 *
 * 키 모양은 [ConsentLocationMarker.key] 와 같다 — 이동은 한 `rawId` 에 점이 둘이라 그 자체로는
 * 어느 쪽 주소인지 말할 수 없어 접미사가 필요하다.
 */
internal fun stayAddressKey(rawId: String): String = rawId

/** 이동 출발점의 덧입힘 키. */
internal fun movementStartAddressKey(rawId: String): String = "$rawId$MOVEMENT_START_SUFFIX"

/** 이동 도착점의 덧입힘 키. */
internal fun movementEndAddressKey(rawId: String): String = "$rawId$MOVEMENT_END_SUFFIX"

/**
 * 현재 생성 시도 스냅샷의 위치 항목을 마커로 옮긴다.
 *
 * `STAY` 는 체류 좌표 하나, `MOVEMENT` 는 시작·종료 좌표 두 개다. 두 좌표를 잇는 선은 그리지
 * 않는다 — [MovementPayload.distanceMeters] 는 샘플 간 누적 거리라 직선 길이와 맞지 않고,
 * 왕복 이동이면 두 점이 겹쳐 선이 사라진다. 직선을 실제 경로로 오해할 여지만 남는다.
 *
 * 위치가 아닌 항목은 조용히 건너뛴다.
 *
 * @param resolvedAddresses 표시 시점에 해석한 주소. 수집 당시 주소가 이미 있으면 그쪽이 먼저다.
 */
internal fun List<SourceItem>.toLocationMarkers(
    zone: ZoneId,
    resolvedAddresses: Map<String, String> = emptyMap(),
): List<ConsentLocationMarker> {
    var order = 0
    return flatMap { item ->
        when (val payload = item.payload) {
            is StayPayload ->
                listOf(
                    ConsentLocationMarker(
                        key = item.rawId,
                        sourceRawId = item.rawId,
                        order = ++order,
                        kind = ConsentLocationMarker.Kind.STAY,
                        latitude = payload.latitude,
                        longitude = payload.longitude,
                        title =
                            payload.address
                                ?: resolvedAddresses[stayAddressKey(item.rawId)]
                                ?: UNRESOLVED_MARKER_LABEL,
                        snippet = formatDateTimeRange(item.startAt, item.endAt, zone),
                    ),
                )

            // 이동은 시작과 도착이 각각 번호를 받는다. 한 번호를 두 핀이 나눠 가지면 지도에서
            // 같은 숫자가 두 곳에 찍혀 어느 쪽이 출발인지 알 수 없다.
            is MovementPayload ->
                listOf(
                    payload.start.toMarker(
                        item,
                        ++order,
                        ConsentLocationMarker.Kind.MOVEMENT_START,
                        MOVEMENT_START_SUFFIX,
                        MOVEMENT_START_LABEL,
                        resolvedAddresses[movementStartAddressKey(item.rawId)],
                        zone,
                    ),
                    payload.end.toMarker(
                        item,
                        ++order,
                        ConsentLocationMarker.Kind.MOVEMENT_END,
                        MOVEMENT_END_SUFFIX,
                        MOVEMENT_END_LABEL,
                        resolvedAddresses[movementEndAddressKey(item.rawId)],
                        zone,
                    ),
                )

            else -> emptyList()
        }
    }
}

private fun GeoPoint.toMarker(
    item: SourceItem,
    order: Int,
    kind: ConsentLocationMarker.Kind,
    suffix: String,
    roleLabel: String,
    resolvedAddress: String?,
    zone: ZoneId,
) = ConsentLocationMarker(
    key = "${item.rawId}$suffix",
    sourceRawId = item.rawId,
    order = order,
    kind = kind,
    latitude = latitude,
    longitude = longitude,
    title = address ?: resolvedAddress ?: UNRESOLVED_MARKER_LABEL,
    snippet = "$roleLabel · ${formatDateTimeRange(item.startAt, item.endAt, zone)}",
)

private const val UNRESOLVED_MARKER_LABEL = "주소 미확인"
private const val MOVEMENT_START_SUFFIX = ":start"
private const val MOVEMENT_END_SUFFIX = ":end"
private const val MOVEMENT_START_LABEL = "이동 시작"
private const val MOVEMENT_END_LABEL = "이동 도착"
