package com.soma369.laimory.core.collection.location

/**
 * 한 줄 주소에서 읽어 낸 층위.
 *
 * `Geocoder` 의 구조화 필드가 비어 나오는 자리를 메운다 — 같은 좌표라도 `오산시 부산동` 은 한
 * 줄에 다 들어 있으면서 `subLocality` 는 비어 오는 식으로, 지역마다 어느 필드가 차는지 제각각이다.
 */
internal data class AddressLineLayers(
    /** 시·군·구 — `오산시`, `강남구`. */
    val city: String?,
    /** 읍·면·동 — `부산동`, `역삼동`. */
    val district: String?,
)

/**
 * 한 줄 주소를 낱말로 끊어 두 층위를 고른다.
 *
 * 꼬리말이 같은 낱말이 여럿이면 **마지막**을 고른다 — `서울특별시 강남구` 는 앞뒤가 모두 시·군·구
 * 꼬리말이고, 뒤로 갈수록 좁은 층위이므로 뒤엣것이 우리가 찾는 값이다.
 */
internal fun addressLineLayers(line: String): AddressLineLayers {
    val words = line.split(' ')
    return AddressLineLayers(
        city = words.lastEndingWith(CITY_SUFFIXES),
        district = words.lastEndingWith(DISTRICT_SUFFIXES),
    )
}

private fun List<String>.lastEndingWith(suffixes: List<String>): String? =
    lastOrNull { word -> word.length > 1 && suffixes.any(word::endsWith) }

/** 시·군·구 꼬리말. `서울특별시` 도 걸리지만 더 좁은 `강남구` 가 뒤에 있어 그것이 이긴다. */
private val CITY_SUFFIXES = listOf("시", "군", "구")

/** 읍·면·동 꼬리말. */
private val DISTRICT_SUFFIXES = listOf("동", "읍", "면", "리", "가")
