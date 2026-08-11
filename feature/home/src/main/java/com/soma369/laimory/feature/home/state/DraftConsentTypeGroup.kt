package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.domain.model.collection.ItemType

/**
 * 동의 화면에 노출하는 데이터 유형 그룹.
 *
 * 화면 목록에서 위치는 STAY·MOVEMENT 를 합산해 한 행으로 보여주고, 상세에서 두 타입을 구분한다.
 */
enum class DraftConsentTypeGroup(
    val memberTypes: List<ItemType>,
) {
    PHOTO(listOf(ItemType.PHOTO)),
    CALENDAR(listOf(ItemType.CALENDAR)),
    LOCATION(listOf(ItemType.STAY, ItemType.MOVEMENT)),
    HEALTH(listOf(ItemType.HEALTH)),
    NOTIFICATION(listOf(ItemType.NOTIFICATION)),
}
