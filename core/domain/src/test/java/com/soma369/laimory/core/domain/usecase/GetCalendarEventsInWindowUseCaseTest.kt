package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.source.CalendarSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class GetCalendarEventsInWindowUseCaseTest {
    private val window =
        RecordDateWindow(
            start = Instant.parse("2026-10-21T21:00:00Z"),
            end = Instant.parse("2026-10-22T21:00:00Z"),
        )

    @Test
    fun `지나간 창은 그대로 읽는다`() =
        runBlocking {
            val source = FakeCalendarSource()

            useCase(source, now = Instant.parse("2026-10-23T00:00:00Z"))(window)

            assertEquals(window, source.requestedWindow)
        }

    @Test
    fun `지금을 걸친 창은 지금까지만 읽어 아직 시작하지 않은 일정을 뺀다`() =
        runBlocking {
            val source = FakeCalendarSource()
            val now = Instant.parse("2026-10-22T05:00:00Z")

            useCase(source, now)(window)

            assertEquals(RecordDateWindow(window.start, now), source.requestedWindow)
        }

    @Test
    fun `아직 시작하지 않은 창은 읽지 않는다`() =
        runBlocking {
            val source = FakeCalendarSource()

            val events = useCase(source, now = window.start)(window)

            assertEquals(emptyList<SourceItem>(), events)
            assertNull(source.requestedWindow)
        }

    private fun useCase(
        source: CalendarSource,
        now: Instant,
    ) = GetCalendarEventsInWindowUseCase(source, Clock.fixed(now, ZoneOffset.UTC))

    private class FakeCalendarSource : CalendarSource {
        var requestedWindow: RecordDateWindow? = null

        override suspend fun eventsIn(window: RecordDateWindow): List<SourceItem> {
            requestedWindow = window
            return emptyList()
        }

        override fun changes(): Flow<Unit> = emptyFlow()
    }
}
