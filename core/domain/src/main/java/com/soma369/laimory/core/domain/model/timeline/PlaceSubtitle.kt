package com.soma369.laimory.core.domain.model.timeline

/**
 * 부제를 첫 장소 자리 기준으로 나눈 결과. 장소를 칩으로 그릴 때 쓴다.
 *
 * 칩은 첫 자리 하나에만 둔다. 뒤에 장소 자리가 더 있으면 [after] 안에서 이미 채워져 있다.
 */
data class PlaceSubtitle(
    val before: String,
    val place: String,
    val after: String,
)
