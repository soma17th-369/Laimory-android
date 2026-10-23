package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.source.CalendarSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GetCalendarEventsInWindowUseCaseTest {
    private val zone = ZoneId.of("Asia/Seoul")

    // 2026-10-22 06:00 ~ 10-23 06:00 KST
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

    @Test
    fun `기록 날짜를 주면 다음 날 종일 일정은 빼고 시간 지정 일정은 남긴다`() =
        runBlocking {
            val recordDate = LocalDate.of(2026, 10, 22)
            val nextDay = recordDate.plusDays(1)
            val source =
                FakeCalendarSource(
                    events =
                        listOf(
                            event("today-all-day", recordDate, allDay = true),
                            event("next-all-day", nextDay, allDay = true),
                            event("next-timed-midnight", nextDay, allDay = false),
                        ),
                )

            val events = GetCalendarEventsInWindowUseCase(source)(window, recordDate, zone)

            assertEquals(listOf("today-all-day", "next-timed-midnight"), events.map(SourceItem::rawId))
        }

    /** [date] 00:00 ~ 익일 00:00 일정. */
    private fun event(
        id: String,
        date: LocalDate,
        allDay: Boolean,
    ): SourceItem {
        val start = date.atStartOfDay(zone).toInstant()
        return SourceItem(
            rawId = id,
            startAt = start,
            endAt = date.plusDays(1).atStartOfDay(zone).toInstant(),
            timeZoneId = zone,
            payload = CalendarPayload(title = id, description = null, locationText = null, allDay = allDay),
            sourceName = SourceName.CALENDAR_PROVIDER,
            sourceKey = id,
            collectedAt = start,
        )
    }

    private class FakeCalendarSource(
        private val events: List<SourceItem> = emptyList(),
    ) : CalendarSource {
        var requestedWindow: RecordDateWindow? = null

        override suspend fun eventsIn(window: RecordDateWindow): List<SourceItem> {
            requestedWindow = window
            return events.filter(window::contains)
        }

        override fun changes(): Flow<Unit> = emptyFlow()
    }
}
