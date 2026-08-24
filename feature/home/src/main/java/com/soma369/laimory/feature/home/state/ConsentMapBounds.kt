package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable

/**
 * 마커 전체를 담는 지도 카메라 영역.
 *
 * 지도 SDK 타입을 쓰지 않는 순수 값이라 JVM 테스트로 검증한다 — 지도는 계측 없이는 확인할 수
 * 없으므로, 검증 가능한 계산은 SDK 밖으로 빼 둔다.
 */
@Immutable
data class ConsentMapBounds(
    val southLatitude: Double,
    val westLongitude: Double,
    val northLatitude: Double,
    val eastLongitude: Double,
) {
    init {
        require(southLatitude <= northLatitude) { "south must not exceed north" }
        require(westLongitude <= eastLongitude) { "west must not exceed east" }
    }
}

/**
 * 마커 전체를 담는 최소 영역을 만든다. 마커가 없으면 null 이다.
 *
 * 마커가 하나뿐이거나 좌표가 모두 같으면 영역의 넓이가 0이 되어 지도가 최대 배율까지 확대된다.
 * 그러면 어디인지 알아보기 어려우므로 [MINIMUM_SPAN_DEGREES] 만큼 여백을 준다.
 *
 * 경도 180도 경계(날짜변경선)를 넘는 묶음은 다루지 않는다 — 하루 기록 창 안의 이동이라 그런
 * 조합이 생기지 않고, 다루려면 지도 SDK 의 영역 계산과 규칙을 맞춰야 한다.
 */
internal fun List<ConsentLocationMarker>.toBounds(): ConsentMapBounds? {
    if (isEmpty()) return null
    val latitudes = map { it.latitude }
    val longitudes = map { it.longitude }
    return ConsentMapBounds(
        southLatitude = latitudes.min() - latitudes.paddingFor(),
        westLongitude = longitudes.min() - longitudes.paddingFor(),
        northLatitude = latitudes.max() + latitudes.paddingFor(),
        eastLongitude = longitudes.max() + longitudes.paddingFor(),
    )
}

/** 폭이 최소치보다 좁으면 양쪽에 나눠 줄 여백. 이미 충분히 넓으면 0이다. */
private fun List<Double>.paddingFor(): Double {
    val span = max() - min()
    return ((MINIMUM_SPAN_DEGREES - span) / 2).coerceAtLeast(0.0)
}

/** 약 110m. 단일 체류나 같은 자리 왕복에서 지도가 과하게 확대되지 않을 정도의 최소 폭. */
private const val MINIMUM_SPAN_DEGREES = 0.001
