package com.soma369.laimory.core.domain.model.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AnalyticsRecordAgeBucketTest {
    /** UTC 로는 9월 20일 저녁이지만 서울로는 이미 9월 21일이다. */
    private val clock = Clock.fixed(Instant.parse("2026-09-20T16:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.parse("2026-09-21")

    @Test
    fun `오늘과 미래는 지난 기록이 아니다`() {
        assertNull(AnalyticsRecordAgeBucket.of(today, clock))
        assertNull(AnalyticsRecordAgeBucket.of(today.plusDays(1), clock))
    }

    @Test
    fun `날짜 차이로 구간을 가른다`() {
        assertEquals(AnalyticsRecordAgeBucket.D1, AnalyticsRecordAgeBucket.of(today.minusDays(1), clock))
        assertEquals(AnalyticsRecordAgeBucket.D2_6, AnalyticsRecordAgeBucket.of(today.minusDays(2), clock))
        assertEquals(AnalyticsRecordAgeBucket.D2_6, AnalyticsRecordAgeBucket.of(today.minusDays(6), clock))
        assertEquals(AnalyticsRecordAgeBucket.D7_29, AnalyticsRecordAgeBucket.of(today.minusDays(7), clock))
        assertEquals(AnalyticsRecordAgeBucket.D7_29, AnalyticsRecordAgeBucket.of(today.minusDays(29), clock))
        assertEquals(AnalyticsRecordAgeBucket.D30_PLUS, AnalyticsRecordAgeBucket.of(today.minusDays(30), clock))
    }
}
