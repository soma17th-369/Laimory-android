package com.soma369.laimory.feature.timeline.component

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TimelineEventSummaryLabelTest {
    private val recordDate: LocalDate = LocalDate.of(2026, 5, 8)

    @Test
    fun `기록 날짜와 같은 날은 시각만 보여준다`() {
        val label = summaryLabel(recordDate, LocalDateTime.of(2026, 5, 8, 8, 35))

        assertEquals("08:35", label)
    }

    @Test
    fun `다음 날이면 익일을 앞에 붙인다`() {
        val label = summaryLabel(recordDate, LocalDateTime.of(2026, 5, 9, 1, 0))

        assertEquals("익일 01:00", label)
    }

    @Test
    fun `시작과 종료가 모두 익일이어도 각각 날짜를 드러낸다`() {
        val start = summaryLabel(recordDate, LocalDateTime.of(2026, 5, 9, 1, 0))
        val end = summaryLabel(recordDate, LocalDateTime.of(2026, 5, 9, 3, 30))

        // 시작 날짜끼리 비교하면 둘 다 평범한 시각으로 보여 고른 날짜를 알 수 없다.
        assertEquals("익일 01:00", start)
        assertEquals("익일 03:30", end)
    }

    @Test
    fun `시트로 고를 수 없는 날짜는 날짜를 그대로 보여준다`() {
        val label = summaryLabel(recordDate, LocalDateTime.of(2026, 5, 10, 9, 0))

        assertEquals("(05.10) 09:00", label)
    }
}
