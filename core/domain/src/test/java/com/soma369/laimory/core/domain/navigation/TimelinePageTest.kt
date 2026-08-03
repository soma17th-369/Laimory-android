package com.soma369.laimory.core.domain.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TimelinePageTest {
    @Test
    fun `경로 인자에 기록 날짜를 ISO 형식으로 저장한다`() {
        val recordDate = LocalDate.of(2026, 8, 3)

        val route = TimelinePage(recordDate).toRoute()

        assertEquals(TimelinePage.PATH, route.path)
        assertEquals("2026-08-03", route.args[TimelinePage.RECORD_DATE_ARG])
    }

    @Test
    fun `유효한 경로 인자에서 기록 날짜를 복원한다`() {
        val recordDate =
            TimelinePage.recordDateFrom(
                mapOf(TimelinePage.RECORD_DATE_ARG to "2026-08-03"),
            )

        assertEquals(LocalDate.of(2026, 8, 3), recordDate)
    }

    @Test
    fun `기록 날짜 인자가 없으면 null을 반환한다`() {
        assertNull(TimelinePage.recordDateFrom(emptyMap()))
    }

    @Test
    fun `기록 날짜 인자가 잘못된 형식이면 null을 반환한다`() {
        val recordDate =
            TimelinePage.recordDateFrom(
                mapOf(TimelinePage.RECORD_DATE_ARG to "2026-02-30"),
            )

        assertNull(recordDate)
    }
}
