package com.soma369.laimory.feature.home.component

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecommendedDateButtonLabelTest {
    @Test
    fun `월과 일을 앞자리 0 없이 쓰고 요일은 붙이지 않는다`() {
        assertEquals("11월 20일로 만들어 보기", recommendedDateButtonLabel(LocalDate.of(2026, 11, 20)))
        assertEquals("1월 3일로 만들어 보기", recommendedDateButtonLabel(LocalDate.of(2027, 1, 3)))
    }
}
