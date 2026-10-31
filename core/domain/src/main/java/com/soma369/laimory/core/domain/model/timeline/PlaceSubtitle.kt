package com.soma369.laimory.core.domain.model.timeline

/**
 * 부제를 장소 자리마다 나눈 결과. 장소를 칩으로 그릴 때 쓴다.
 *
 * [texts] 는 장소 자리 사이의 글 조각이라 자리 수보다 하나 많다 — 자리가 하나면 앞·뒤 두 조각이다. 이벤트의
 * 장소는 하나라 모든 자리가 같은 [place] 를 가리킨다.
 */
data class PlaceSubtitle(
    val texts: List<String>,
    val place: String,
) {
    /** 모든 자리를 [place] 로 채운 문장. */
    val text: String
        get() = texts.joinToString(separator = place)
}
