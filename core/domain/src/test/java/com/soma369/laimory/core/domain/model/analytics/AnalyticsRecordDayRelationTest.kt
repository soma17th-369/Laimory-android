package com.soma369.laimory.core.domain.model.analytics

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AnalyticsRecordDayRelationTest {
    // UTC 9/20 16:00 = 서울 9/21 01:00. 기기 시간대(UTC)로 보면 오늘이 9/20 이라 어긋나는 시각이다.
    private val clock = Clock.fixed(Instant.parse("2026-09-20T16:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `날짜 경계는 기기 시간대가 아니라 서울로 본다`() {
        assertEquals(AnalyticsRecordDayRelation.TODAY, relationOf("2026-09-21"))
        assertEquals(AnalyticsRecordDayRelation.YESTERDAY, relationOf("2026-09-20"))
    }

    @Test
    fun `이틀 이상 지난 날은 과거, 오늘 뒤는 미래다`() {
        assertEquals(AnalyticsRecordDayRelation.OLDER, relationOf("2026-09-19"))
        assertEquals(AnalyticsRecordDayRelation.FUTURE, relationOf("2026-09-22"))
    }

    private fun relationOf(date: String) = AnalyticsRecordDayRelation.of(LocalDate.parse(date), clock)
}
