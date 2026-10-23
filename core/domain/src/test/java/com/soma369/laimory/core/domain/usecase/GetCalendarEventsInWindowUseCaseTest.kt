package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.source.CalendarSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class GetCalendarEventsInWindowUseCaseTest {
    private val window =
        RecordDateWindow(
            start = Instant.parse("2026-10-21T21:00:00Z"),
            end = Instant.parse("2026-10-22T21:00:00Z"),
        )

    @Test
    fun `지금을 걸친 창도 자르지 않고 그대로 읽어 아직 시작하지 않은 일정까지 넣는다`() =
        runBlocking {
            val source = FakeCalendarSource()

            GetCalendarEventsInWindowUseCase(source)(window)

            assertEquals(window, source.requestedWindow)
        }

    private class FakeCalendarSource : CalendarSource {
        var requestedWindow: RecordDateWindow? = null

        override suspend fun eventsIn(window: RecordDateWindow): List<SourceItem> {
            requestedWindow = window
            return emptyList()
        }

        override fun changes(): Flow<Unit> = emptyFlow()
    }
}
