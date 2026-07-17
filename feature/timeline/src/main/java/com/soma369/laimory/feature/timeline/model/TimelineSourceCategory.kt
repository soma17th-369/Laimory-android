package com.soma369.laimory.feature.timeline.model

import com.soma369.laimory.core.domain.model.collection.ItemType

/**
 * 타임라인 요약 패널의 표시 카테고리. 선언 순서 = 표시 순서다.
 *
 * [LOCATION] 은 상위 개념 "위치" 로 체류(`ItemType.STAY`)와 이동(`ItemType.MOVEMENT`)을
 * 한 줄에 합산한다 — 수집 실험실 "위치" 탭 라벨과 용어를 맞춘다.
 */
enum class TimelineSourceCategory(
    val label: String,
    val itemTypes: Set<ItemType>,
) {
    PHOTO("사진", setOf(ItemType.PHOTO)),
    CALENDAR("일정", setOf(ItemType.CALENDAR)),
    HEALTH("헬스", setOf(ItemType.HEALTH)),
    LOCATION("위치", setOf(ItemType.STAY, ItemType.MOVEMENT)),
    NOTIFICATION("알림", setOf(ItemType.NOTIFICATION)),
}
