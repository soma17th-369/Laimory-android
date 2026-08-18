package com.soma369.laimory.feature.timeline.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TimelineEmotionDateLabelTest {
    private val today: LocalDate = LocalDate.of(2026, 5, 20)

    @Test
    fun `오늘과 어제는 말로 짚는다`() {
        assertEquals("오늘", timelineEmotionDateLabel(recordDate = today, today = today))
        assertEquals("어제", timelineEmotionDateLabel(recordDate = today.minusDays(1), today = today))
    }

    @Test
    fun `그보다 지난 날짜는 MM_DD 로 명시한다`() {
        assertEquals("05.14", timelineEmotionDateLabel(recordDate = LocalDate.of(2026, 5, 14), today = today))
        assertEquals("12.31", timelineEmotionDateLabel(recordDate = LocalDate.of(2025, 12, 31), today = today))
    }

    @Test
    fun `달과 해가 바뀌어도 하루 차이는 어제로 읽는다`() {
        val newYear = LocalDate.of(2026, 1, 1)
        assertEquals("어제", timelineEmotionDateLabel(recordDate = LocalDate.of(2025, 12, 31), today = newYear))
    }

    @Test
    fun `아직 오지 않은 날짜도 날짜 표기로 떨어진다`() {
        assertEquals("05.21", timelineEmotionDateLabel(recordDate = today.plusDays(1), today = today))
    }
}
