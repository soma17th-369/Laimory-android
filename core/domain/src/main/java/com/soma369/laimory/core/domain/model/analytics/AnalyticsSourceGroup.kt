package com.soma369.laimory.core.domain.model.analytics

import com.soma369.laimory.core.domain.model.collection.ItemType

/**
 * 종류별 건수를 셀 묶음. 확인 창("타임라인을 만들까요?")이 보여 주는 묶음과 같다 — 사용자가 본 숫자와
 * 분석 숫자가 어긋나지 않게 한다. 머문 곳과 이동은 위치 하나로 본다.
 */
enum class AnalyticsSourceGroup {
    PHOTO,
    CALENDAR,
    LOCATION,
    HEALTH,
    NOTIFICATION,
    ;

    companion object {
        fun of(itemType: ItemType): AnalyticsSourceGroup =
            when (itemType) {
                ItemType.PHOTO -> PHOTO
                ItemType.CALENDAR -> CALENDAR
                ItemType.STAY, ItemType.MOVEMENT -> LOCATION
                ItemType.HEALTH -> HEALTH
                ItemType.NOTIFICATION -> NOTIFICATION
            }
    }
}
