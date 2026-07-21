package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class HomeUiStateTest {
    private val date = LocalDate.of(2026, 7, 20)
    private val zone = ZoneId.of("Asia/Seoul")

    @Test
    fun `기본 선택은 오늘 자정부터 익일 자정까지다`() {
        val state = HomeUiState(selectedDate = date)

        val window = state.recordDateWindow(zone)

        assertNotNull(window)
        assertEquals(date.atStartOfDay(zone).toInstant(), window!!.start)
        assertEquals(date.plusDays(1).atStartOfDay(zone).toInstant(), window.end)
    }

    @Test
    fun `익일 종료 시각을 선택할 수 있다`() {
        val state =
            HomeUiState(
                selectedDate = date,
                startTime = LocalTime.of(9, 0),
                endDay = DraftEndDay.NEXT_DAY,
                endTime = LocalTime.of(2, 0),
            )

        val window = state.recordDateWindow(zone)

        assertEquals(date.atTime(9, 0).atZone(zone).toInstant(), window!!.start)
        assertEquals(date.plusDays(1).atTime(2, 0).atZone(zone).toInstant(), window.end)
    }

    @Test
    fun `당일 종료가 시작보다 이르거나 같으면 유효하지 않다`() {
        val same =
            HomeUiState(
                selectedDate = date,
                startTime = LocalTime.of(9, 0),
                endDay = DraftEndDay.SAME_DAY,
                endTime = LocalTime.of(9, 0),
            )
        val before = same.copy(endTime = LocalTime.of(8, 59))

        assertNull(same.recordDateWindow(zone))
        assertNull(before.recordDateWindow(zone))
    }

    @Test
    fun `익일 자정 상태에서 당일을 선택하면 종료 시각을 23시 59분으로 보정한다`() {
        val state =
            HomeUiState(
                selectedDate = date,
                startTime = LocalTime.MIDNIGHT,
                endDay = DraftEndDay.NEXT_DAY,
                endTime = LocalTime.MIDNIGHT,
            )

        val adjusted = state.withEndDaySelection(DraftEndDay.SAME_DAY)

        assertEquals(DraftEndDay.SAME_DAY, adjusted.endDay)
        assertEquals(LocalTime.of(23, 59), adjusted.endTime)
        assertNotNull(adjusted.recordDateWindow(zone))
    }

    @Test
    fun `당일 종료가 이미 시작보다 뒤라면 기존 종료 시각을 유지한다`() {
        val state =
            HomeUiState(
                selectedDate = date,
                startTime = LocalTime.of(9, 0),
                endDay = DraftEndDay.NEXT_DAY,
                endTime = LocalTime.of(18, 0),
            )

        val adjusted = state.withEndDaySelection(DraftEndDay.SAME_DAY)

        assertEquals(DraftEndDay.SAME_DAY, adjusted.endDay)
        assertEquals(LocalTime.of(18, 0), adjusted.endTime)
    }

    @Test
    fun `선택 범위 안의 사진을 기본으로 모두 선택하고 최신 사진부터 보여준다`() {
        val items =
            listOf(
                item("early", date.atTime(8, 0), photo("early")),
                item("late", date.atTime(20, 0), photo("late")),
                item("calendar", date.atTime(12, 0), CalendarPayload("일정", null, null, false)),
                item("outside", date.plusDays(1).atTime(1, 0), photo("outside")),
            )

        val state = HomeUiState(selectedDate = date).refreshSourceSummary(items, zone)

        assertEquals(setOf("early", "late"), state.selectedPhotoIds)
        assertEquals(2, state.summary.photoCount)
        assertEquals(listOf("content://late", "content://early"), state.summary.photoPreviewUris)
        assertEquals(3, state.summary.totalItemCount)
    }

    @Test
    fun `사진 선택을 확정하면 선택한 사진과 비사진 데이터만 초안에 포함한다`() {
        val items =
            listOf(
                item("photo-1", date.atTime(8, 0), photo("photo-1")),
                item("photo-2", date.atTime(9, 0), photo("photo-2")),
                item("calendar", date.atTime(10, 0), CalendarPayload("일정", null, null, false)),
            )
        val initial = HomeUiState(selectedDate = date).refreshSourceSummary(items, zone)
        val selected =
            initial
                .copy(
                    selectedPhotoIds = setOf("photo-2"),
                    hasCustomizedPhotoSelection = true,
                ).refreshSourceSummary(items, zone)

        assertEquals(1, selected.summary.photoCount)
        assertEquals(2, selected.summary.totalItemCount)
        assertEquals(
            setOf("photo-2", "calendar"),
            selected.selectedSourceItems(items, zone).mapTo(mutableSetOf(), SourceItem::rawId),
        )
    }

    private fun photo(id: String) = PhotoPayload("$id.jpg", "content://$id", null, null, null)

    private fun item(
        id: String,
        dateTime: LocalDateTime,
        payload: SourceItemPayload,
    ): SourceItem =
        SourceItem(
            rawId = id,
            startAt = dateTime.atZone(zone).toInstant(),
            endAt = null,
            timeZoneId = zone,
            payload = payload,
            sourceName = SourceName.MEDIA_STORE,
            sourceKey = id,
            collectedAt = dateTime.atZone(zone).toInstant(),
        )
}
