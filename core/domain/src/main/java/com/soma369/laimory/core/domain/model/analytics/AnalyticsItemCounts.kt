package com.soma369.laimory.core.domain.model.analytics

import com.soma369.laimory.core.domain.model.collection.ItemType

/**
 * 묶음별 항목 수와 그 합계.
 *
 * 없는 묶음도 0 으로 센다 — 빠뜨리면 "일정 0개"와 "기록되지 않음"이 구분되지 않는다.
 */
data class AnalyticsItemCounts(
    private val byGroup: Map<AnalyticsSourceGroup, Int>,
) {
    val total: Int get() = byGroup.values.sum()

    fun countOf(group: AnalyticsSourceGroup): Int = byGroup[group] ?: 0

    companion object {
        fun of(itemTypes: Iterable<ItemType>): AnalyticsItemCounts =
            AnalyticsItemCounts(itemTypes.groupingBy(AnalyticsSourceGroup::of).eachCount())
    }
}
