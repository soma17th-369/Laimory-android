package com.soma369.laimory.core.domain.usecase.tutorial

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.settings.DefaultRecordRange
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.source.CalendarSource
import com.soma369.laimory.core.domain.source.PhotoSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class RecommendTutorialDatesUseCaseTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val today = LocalDate.of(2026, 10, 25)
    private val photoSource = FakePhotoSource()
    private val calendarSource = FakeCalendarSource()
    private val useCase = RecommendTutorialDatesUseCase(photoSource, calendarSource)

    @Test
    fun `직접 찍은 사진이 많은 순으로 세 날을 고른다`() =
        runTest {
            photoSource.photos =
                photosOn(today.minusDays(1), count = 2) +
                photosOn(today.minusDays(2), count = 5) +
                photosOn(today.minusDays(3), count = 1) +
                photosOn(today.minusDays(4), count = 4)

            val result = recommend()

            assertEquals(listOf(today.minusDays(2), today.minusDays(4), today.minusDays(1)), result.map { it.date })
            assertEquals(listOf(5, 4, 2), result.map { it.capturedPhotoCount })
        }

    @Test
    fun `오늘은 사진이 가장 많아도 추천하지 않는다`() =
        runTest {
            photoSource.photos = photosOn(today, count = 10) + photosOn(today.minusDays(1), count = 1)

            val result = recommend()

            assertEquals(listOf(today.minusDays(1)), result.map { it.date })
        }

    @Test
    fun `오늘을 포함해 30일보다 앞선 날은 보지 않는다`() =
        runTest {
            photoSource.photos = photosOn(today.minusDays(29), count = 1) + photosOn(today.minusDays(30), count = 9)

            val result = recommend()

            assertEquals(listOf(today.minusDays(29)), result.map { it.date })
        }

    @Test
    fun `보존 기간이 한 달보다 짧으면 그 안에서만 고른다`() =
        runTest {
            photoSource.photos = photosOn(today.minusDays(2), count = 1) + photosOn(today.minusDays(3), count = 9)

            val result = recommend(retentionDays = 3)

            assertEquals(listOf(today.minusDays(2)), result.map { it.date })
        }

    @Test
    fun `이미 기록이 있는 날과 사진이 없는 날은 뺀다`() =
        runTest {
            photoSource.photos = photosOn(today.minusDays(1), count = 3) + photosOn(today.minusDays(2), count = 1)
            calendarSource.events = listOf(event(today.minusDays(3).atTime(10, 0)))

            val result = recommend(excludedDates = setOf(today.minusDays(1)))

            assertEquals(listOf(today.minusDays(2)), result.map { it.date })
        }

    @Test
    fun `사진 수가 같으면 일정이 많은 날, 그래도 같으면 최근 날이 앞이다`() =
        runTest {
            photoSource.photos =
                photosOn(today.minusDays(1), count = 2) +
                photosOn(today.minusDays(2), count = 2) +
                photosOn(today.minusDays(3), count = 2)
            calendarSource.events = listOf(event(today.minusDays(3).atTime(10, 0)))

            val result = recommend()

            assertEquals(listOf(today.minusDays(3), today.minusDays(1), today.minusDays(2)), result.map { it.date })
            assertEquals(listOf(1, 0, 0), result.map { it.calendarEventCount })
        }

    @Test
    fun `건수는 그 날의 기록 범위로 센다`() =
        runTest {
            val date = today.minusDays(2)
            // 기본 범위 06:00 ~ 익일 06:00 — 당일 05:00 은 전날 것이고, 익일 05:00 은 이 날 것이다.
            photoSource.photos =
                listOf(
                    photo(1, date.atTime(5, 0)),
                    photo(2, date.atTime(7, 0)),
                    photo(3, date.plusDays(1).atTime(5, 0)),
                )

            val result = recommend()

            val counted = result.single { it.date == date }
            assertEquals(2, counted.capturedPhotoCount)
            assertEquals(1, result.single { it.date == date.minusDays(1) }.capturedPhotoCount)
        }

    @Test
    fun `대표 사진은 그 날 마지막으로 찍은 사진이다`() =
        runTest {
            val date = today.minusDays(1)
            photoSource.photos = listOf(photo(1, date.atTime(9, 0)), photo(2, date.atTime(21, 0)))

            val result = recommend()

            assertEquals("content://photo/2", result.single().coverPhotoUri)
        }

    @Test
    fun `종일 일정은 그 날짜 기록에만 센다`() =
        runTest {
            val date = today.minusDays(2)
            photoSource.photos = photosOn(date, count = 1) + photosOn(date.plusDays(1), count = 1)
            // 다음 날 종일 일정은 기록 창(06:00 ~ 익일 06:00)과 겹치지만 이 날 기록에 넣지 않는다.
            calendarSource.events = listOf(allDayEvent(date.plusDays(1)))

            val result = recommend()

            assertEquals(0, result.single { it.date == date }.calendarEventCount)
            assertEquals(1, result.single { it.date == date.plusDays(1) }.calendarEventCount)
        }

    @Test
    fun `일정을 읽지 못해도 사진으로 추천한다`() =
        runTest {
            photoSource.photos = photosOn(today.minusDays(1), count = 1)
            calendarSource.failure = IOException("calendar")

            val result = recommend()

            assertEquals(listOf(0), result.map { it.calendarEventCount })
        }

    @Test
    fun `한 달을 사진 한 번 일정 한 번으로 읽는다`() =
        runTest {
            photoSource.photos = photosOn(today.minusDays(1), count = 1)

            recommend()

            val window = photoSource.requestedWindows.single()
            assertEquals(today.minusDays(29).atTime(6, 0).atZone(zone).toInstant(), window.start)
            assertEquals(today.atTime(6, 0).atZone(zone).toInstant(), window.end)
            assertEquals(listOf(window), calendarSource.requestedWindows)
        }

    @Test
    fun `찍은 사진이 하나도 없으면 일정을 읽지 않고 비운다`() =
        runTest {
            calendarSource.events = listOf(event(today.minusDays(1).atTime(10, 0)))

            val result = recommend()

            assertTrue(result.isEmpty())
            assertTrue(calendarSource.requestedWindows.isEmpty())
        }

    private suspend fun recommend(
        retentionDays: Int? = 30,
        excludedDates: Set<LocalDate> = emptySet(),
        range: DefaultRecordRange = DefaultRecordRange.INITIAL,
    ) = useCase(today, zone, range, retentionDays, excludedDates)

    private var nextPhotoId = 100L

    private fun photosOn(
        date: LocalDate,
        count: Int,
    ): List<PhotoCandidate> = List(count) { photo(nextPhotoId++, date.atTime(LocalTime.NOON).plusMinutes(it.toLong())) }

    private fun photo(
        id: Long,
        takenAt: LocalDateTime,
    ) = PhotoCandidate(id = id, contentUri = "content://photo/$id", takenAt = takenAt.atZone(zone).toInstant())

    private fun event(start: LocalDateTime): SourceItem =
        calendarItem(
            start.atZone(zone).toInstant(),
            start.plusHours(1).atZone(zone).toInstant(),
            allDay = false,
        )

    private fun allDayEvent(date: LocalDate): SourceItem =
        calendarItem(date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant(), allDay = true)

    private fun calendarItem(
        start: Instant,
        end: Instant,
        allDay: Boolean,
    ) = SourceItem(
        rawId = "calendar-$start",
        startAt = start,
        endAt = end,
        timeZoneId = zone,
        payload = CalendarPayload(title = "일정", description = null, locationText = null, allDay = allDay),
        sourceName = SourceName.CALENDAR_PROVIDER,
        sourceKey = "calendar-$start",
        collectedAt = start,
    )

    private class FakePhotoSource : PhotoSource {
        var photos: List<PhotoCandidate> = emptyList()
        val requestedWindows = mutableListOf<RecordDateWindow>()

        override suspend fun photosIn(window: RecordDateWindow): List<PhotoCandidate> = emptyList()

        override suspend fun capturedPhotosIn(window: RecordDateWindow): List<PhotoCandidate> {
            requestedWindows += window
            return photos.filter { it.takenAt >= window.start && it.takenAt < window.end }.sortedByDescending { it.takenAt }
        }

        override suspend fun collect(ids: List<Long>): List<SourceItem> = emptyList()
    }

    private class FakeCalendarSource : CalendarSource {
        var events: List<SourceItem> = emptyList()
        var failure: Exception? = null
        val requestedWindows = mutableListOf<RecordDateWindow>()

        override suspend fun eventsIn(window: RecordDateWindow): List<SourceItem> {
            requestedWindows += window
            failure?.let { throw it }
            return events.filter(window::contains)
        }

        override fun changes(): Flow<Unit> = emptyFlow()
    }
}
