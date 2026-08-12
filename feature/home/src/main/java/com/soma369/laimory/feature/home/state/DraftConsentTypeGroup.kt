package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.domain.model.collection.ItemType

/**
 * 동의 화면에 노출하는 데이터 유형 그룹.
 *
 * 화면 목록에서 위치는 STAY·MOVEMENT 를 합산해 한 행으로 보여주고, 상세에서 두 타입을 구분한다.
 * [countUnit]은 Figma 표기를 따른다 — 사진 "장", 일정 "개", 나머지 "건".
 */
enum class DraftConsentTypeGroup(
    val memberTypes: List<ItemType>,
    val countUnit: String,
) {
    PHOTO(listOf(ItemType.PHOTO), "장"),
    CALENDAR(listOf(ItemType.CALENDAR), "개"),
    LOCATION(listOf(ItemType.STAY, ItemType.MOVEMENT), "건"),
    HEALTH(listOf(ItemType.HEALTH), "건"),
    NOTIFICATION(listOf(ItemType.NOTIFICATION), "건"),
}
