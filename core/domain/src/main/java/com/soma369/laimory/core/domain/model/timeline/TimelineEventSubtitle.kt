package com.soma369.laimory.core.domain.model.timeline

/** 서버가 부제의 장소 자리에 넣는 자리 표시. 표시할 때 지금 장소로 채운다. */
const val PLACE_PLACEHOLDER = "{place}"

/**
 * 장소 자리를 채운 부제. 부제가 없으면 null 이다.
 *
 * 장소가 비었으면 주소로, 그것도 없으면 자리를 지우고 앞뒤 공백을 정리한다. 서버가 장소 없는 자리
 * 표시를 거절하므로 정상 응답에선 일어나지 않지만, 자리 표시가 화면에 그대로 보이는 것보다 낫다.
 */
val TimelineEvent.displaySubtitle: String?
    get() {
        val text = subtitle ?: return null
        if (PLACE_PLACEHOLDER !in text) return text
        val fill = place.nonBlank() ?: address.nonBlank()
        return fill?.let { text.replace(PLACE_PLACEHOLDER, it) } ?: text.replace(PLACE_PLACEHOLDER, "").trim()
    }

/**
 * 장소 자리마다 나눈 부제. 자리 표시가 없거나 [TimelineEvent.place] 가 비었으면 null 이다.
 *
 * 주소로 대신 채우지 않는다 — 칩은 바꿀 수 있는 장소를 보여 주는 자리라 편집할 수 없는 주소를 올리면
 * 누른 뒤 고른 값과 칩이 어긋난다.
 */
fun TimelineEvent.placeSubtitle(): PlaceSubtitle? = placeSubtitleOf(subtitle, place)

/**
 * 부제 원문 [subtitle] 을 장소 자리마다 나눈다. 자리가 몇 개든 모두 같은 [place] 를 가리킨다 — 이벤트의 장소는
 * 하나다.
 *
 * 화면이 응답을 기다리며 새 장소를 먼저 보여 줄 때도 이 함수로 다시 채운다. 채운 문장에서 옛 장소 글자를
 * 찾아 바꾸면, 사용자가 직접 쓴 같은 글자까지 바뀐다.
 */
fun placeSubtitleOf(
    subtitle: String?,
    place: String?,
): PlaceSubtitle? {
    val texts = subtitle?.split(PLACE_PLACEHOLDER)?.takeIf { it.size > 1 } ?: return null
    val place = place.nonBlank() ?: return null
    return PlaceSubtitle(texts = texts, place = place)
}

private fun String?.nonBlank(): String? = this?.trim()?.takeIf(String::isNotEmpty)
