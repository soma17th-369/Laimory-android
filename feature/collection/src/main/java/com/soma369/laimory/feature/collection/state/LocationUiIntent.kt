package com.soma369.laimory.feature.collection.state

import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.ui.base.UiIntent

sealed interface LocationUiIntent : UiIntent {
    /** 위치 자동 수집(추적)을 켜거나 끈다. */
    data class SetTracking(val enabled: Boolean) : LocationUiIntent

    /** 스테이징된 위치 계열을 모두 비운다(일괄 삭제). */
    data object ClearStaged : LocationUiIntent

    /** 화면에 노출된 STAY 좌표의 로컬 주소 해석을 요청한다. */
    data class ResolveStayAddress(
        val rawId: String,
        val latitude: Double,
        val longitude: Double,
    ) : LocationUiIntent

    /** 화면에 노출된 MOVEMENT 출발·도착 좌표의 로컬 주소 해석을 요청한다. */
    data class ResolveMovementAddresses(
        val rawId: String,
        val start: GeoPoint,
        val end: GeoPoint,
    ) : LocationUiIntent
}
