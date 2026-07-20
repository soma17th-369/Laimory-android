package com.soma369.laimory.core.domain.model.timeline

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DefaultRecordDateTest {
    @Test
    fun `정오 이전이면 어제가 기본 선택 날짜다`() {
        // 다음날 아침에 어제 일기를 쓰는 UX — 새벽·오전은 전날로 귀속된 하루를 기본으로 보여준다.
        assertEquals(
            LocalDate.of(2026, 7, 8),
            DefaultRecordDate.at(LocalDateTime.of(2026, 7, 9, 9, 30)),
        )
        assertEquals(
            LocalDate.of(2026, 7, 8),
            DefaultRecordDate.at(LocalDateTime.of(2026, 7, 9, 0, 0)),
        )
        assertEquals(
            LocalDate.of(2026, 7, 8),
            DefaultRecordDate.at(LocalDateTime.of(2026, 7, 9, 11, 59)),
        )
    }

    @Test
    fun `정오부터는 오늘이 기본 선택 날짜다`() {
        assertEquals(
            LocalDate.of(2026, 7, 9),
            DefaultRecordDate.at(LocalDateTime.of(2026, 7, 9, 12, 0)),
        )
        assertEquals(
            LocalDate.of(2026, 7, 9),
            DefaultRecordDate.at(LocalDateTime.of(2026, 7, 9, 23, 59)),
        )
    }
}
