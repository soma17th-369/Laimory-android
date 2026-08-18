package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `당일 종료가 시작보다 앞이면 시트 확인을 막는다`() {
        val sheet =
            HomeTimeSheetState(
                startTime = LocalTime.of(9, 0),
                endDay = DraftEndDay.SAME_DAY,
                endTime = LocalTime.of(8, 0),
                expandedField = null,
            )

        assertFalse(sheet.isConfirmEnabled)
        assertTrue(sheet.copy(endTime = LocalTime.of(9, 30)).isConfirmEnabled)
    }

    @Test
    fun `종료가 익일이면 시각과 무관하게 확인할 수 있다`() {
        val sheet =
            HomeTimeSheetState(
                startTime = LocalTime.MIDNIGHT,
                endDay = DraftEndDay.NEXT_DAY,
                endTime = LocalTime.MIDNIGHT,
                expandedField = null,
            )

        assertTrue(sheet.isConfirmEnabled)
    }

    @Test
    fun `MediaStore 후보는 최신순으로 표시하되 사용자가 확정하기 전에는 선택하지 않는다`() {
        val items =
            listOf(
                item("calendar", date.atTime(12, 0), CalendarPayload("일정", null, null, false)),
                item("staged-photo", date.atTime(10, 0), photo("staged")),
            )
        val candidates =
            listOf(
                candidate(id = 1L, dateTime = date.atTime(8, 0)),
                candidate(id = 2L, dateTime = date.atTime(20, 0)),
                candidate(id = 3L, dateTime = date.plusDays(1).atTime(1, 0)),
            )

        val state = HomeUiState(selectedDate = date).refreshSourceSummary(items, candidates, zone)

        assertEquals(listOf(2L, 1L), state.availablePhotos.map(HomePhotoItem::mediaStoreId))
        assertEquals(emptySet<Long>(), state.selectedPhotoIds)
        assertEquals(0, state.summary.photoCount)
        assertEquals(emptyList<String>(), state.summary.photoPreviewUris)
        assertEquals(1, state.summary.totalItemCount)
    }

    @Test
    fun `사용자 지정 범위는 기준일과 익일 사진만 반열린 구간으로 필터링한다`() {
        val state =
            HomeUiState(
                selectedDate = date,
                startTime = LocalTime.of(22, 0),
                endDay = DraftEndDay.NEXT_DAY,
                endTime = LocalTime.of(2, 0),
            )
        val candidates =
            listOf(
                candidate(id = 1L, dateTime = date.atTime(21, 59)),
                candidate(id = 2L, dateTime = date.atTime(23, 0)),
                candidate(id = 3L, dateTime = date.plusDays(1).atTime(1, 0)),
                candidate(id = 4L, dateTime = date.plusDays(1).atTime(2, 0)),
            )

        val refreshed = state.refreshSourceSummary(emptyList(), candidates, zone)

        assertEquals(listOf(3L, 2L), refreshed.availablePhotos.map(HomePhotoItem::mediaStoreId))
        assertEquals(
            setOf(date, date.plusDays(1)),
            refreshed.availablePhotos.mapTo(linkedSetOf()) { it.capturedAt.atZone(zone).toLocalDate() },
        )
    }

    @Test
    fun `확정한 MediaStore 사진만 요약하고 Room PHOTO는 초안 입력에서 제외한다`() {
        val items =
            listOf(
                item("staged-photo", date.atTime(8, 0), photo("staged-photo")),
                item("calendar", date.atTime(10, 0), CalendarPayload("일정", null, null, false)),
            )
        val candidates =
            listOf(
                candidate(id = 1L, dateTime = date.atTime(8, 0)),
                candidate(id = 2L, dateTime = date.atTime(9, 0)),
            )
        val selected =
            HomeUiState(
                selectedDate = date,
                selectedPhotoIds = setOf(2L),
            ).refreshSourceSummary(items, candidates, zone)

        assertEquals(1, selected.summary.photoCount)
        assertEquals(2, selected.summary.totalItemCount)
        assertEquals(listOf("content://photo/2"), selected.summary.photoPreviewUris)
        assertEquals(listOf("calendar"), selected.nonPhotoSourceItems(items, zone).map(SourceItem::rawId))
    }

    private fun photo(id: String) = PhotoPayload("$id.jpg", "content://$id", null, null, null)

    private fun candidate(
        id: Long,
        dateTime: LocalDateTime,
    ) = PhotoCandidate(
        id = id,
        contentUri = "content://photo/$id",
        takenAt = dateTime.atZone(zone).toInstant(),
    )

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
