package com.soma369.laimory.core.domain.model.analytics

import com.soma369.laimory.core.domain.model.collection.ItemType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AnalyticsItemCountsTest {
    @Test
    fun `확인 창과 같은 묶음으로 세고 머문 곳과 이동은 위치로 합친다`() {
        val counts = AnalyticsItemCounts.of(listOf(ItemType.STAY, ItemType.MOVEMENT, ItemType.CALENDAR, ItemType.PHOTO))

        assertEquals(2, counts.countOf(AnalyticsSourceGroup.LOCATION))
        assertEquals(1, counts.countOf(AnalyticsSourceGroup.CALENDAR))
        assertEquals(1, counts.countOf(AnalyticsSourceGroup.PHOTO))
        assertEquals(4, counts.total)
    }

    @Test
    fun `없는 묶음은 0 이다`() {
        val counts = AnalyticsItemCounts.of(listOf(ItemType.CALENDAR))

        assertEquals(0, counts.countOf(AnalyticsSourceGroup.HEALTH))
        assertEquals(0, counts.countOf(AnalyticsSourceGroup.NOTIFICATION))
    }

    @Test
    fun `뺀 수는 최종 상태로만 센다`() {
        val event =
            AnalyticsEvent.TimelineEventReviewCompleted(
                recordDayRelation = AnalyticsRecordDayRelation.TODAY,
                recordDate = LocalDate.parse("2026-09-21"),
                initialCounts = AnalyticsItemCounts.of(listOf(ItemType.CALENDAR, ItemType.CALENDAR, ItemType.NOTIFICATION)),
                finalCounts = AnalyticsItemCounts.of(listOf(ItemType.CALENDAR, ItemType.NOTIFICATION)),
            )

        assertEquals(1, event.netRemovedItemCount)
    }
}
