package com.soma369.laimory.core.domain.model.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalyticsUpdateScopeTest {
    @Test
    fun `바뀐 칸이 없으면 범위도 없다`() {
        assertNull(AnalyticsUpdateScope.of(emptySet()))
    }

    @Test
    fun `같은 범위의 칸만 바뀌면 그 범위다`() {
        assertEquals(
            AnalyticsUpdateScope.CONTENT,
            AnalyticsUpdateScope.of(setOf(AnalyticsEventField.TITLE, AnalyticsEventField.SUBTITLE, AnalyticsEventField.EVENT_TYPE)),
        )
        assertEquals(AnalyticsUpdateScope.TIME, AnalyticsUpdateScope.of(setOf(AnalyticsEventField.START_AT, AnalyticsEventField.END_AT)))
    }

    @Test
    fun `두 범위 이상에 걸치면 묶음이다`() {
        assertEquals(AnalyticsUpdateScope.COMBINED, AnalyticsUpdateScope.of(setOf(AnalyticsEventField.TITLE, AnalyticsEventField.MEMO)))
    }

    @Test
    fun `메모 글자 수는 이모지도 한 글자로 센다`() {
        assertEquals(0, AnalyticsEvent.TimelineMemoSaved.lengthOf(null))
        assertEquals(3, AnalyticsEvent.TimelineMemoSaved.lengthOf("점심\uD83C\uDF5A"))
    }
}
