package com.soma369.laimory.core.domain.model.timeline

/**
 * AI 가 준 장소 후보 하나.
 *
 * 좌표는 장소의 실제 위치다. 없을 수 있고, [longitude] 와 항상 함께 null 이거나 함께 값이다. 후보를 고르면 이름과
 * 함께 좌표를 보내 이벤트 좌표가 그 장소가 된다.
 */
data class PlaceCandidate(
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
