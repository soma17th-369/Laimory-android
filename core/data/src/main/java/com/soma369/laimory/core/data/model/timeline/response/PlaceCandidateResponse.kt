package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.model.timeline.PlaceCandidate
import kotlinx.serialization.Serializable

/** Event 장소 후보 하나. 좌표는 장소의 실제 위치이고 없으면 null 쌍이다(좌표 미배포 서버는 키가 없다). */
@Serializable
data class PlaceCandidateResponse(
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

/**
 * 이름이 비면 후보째 버린다. 좌표는 쌍이 맞고 범위 안일 때만 쓴다 — 아니면 좌표만 버리고 이름은 남긴다.
 *
 * 서버는 범위 밖 좌표를 받지 않지만, 고른 후보의 좌표는 장소 지정 요청으로 그대로 돌아가므로 앱이 한 번 더 걸러
 * 400 을 부르지 않게 한다.
 */
internal fun PlaceCandidateResponse.toDomainOrNull(): PlaceCandidate? {
    if (name.isBlank()) return null
    val hasValidCoordinate =
        latitude != null &&
            longitude != null &&
            latitude in LATITUDE_RANGE &&
            longitude in LONGITUDE_RANGE
    return PlaceCandidate(
        name = name,
        latitude = latitude.takeIf { hasValidCoordinate },
        longitude = longitude.takeIf { hasValidCoordinate },
    )
}

private val LATITUDE_RANGE = -90.0..90.0
private val LONGITUDE_RANGE = -180.0..180.0
