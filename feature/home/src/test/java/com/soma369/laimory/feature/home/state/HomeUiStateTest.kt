package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.collection.StayPayload
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
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
    fun `기록 범위가 6시간 미만이면 확인을 막고 창도 만들지 않는다`() {
        val sheet =
            HomeTimeSheetState(
                recordDate = date,
                startTime = LocalTime.of(9, 0),
                endDay = DraftEndDay.SAME_DAY,
                endTime = LocalTime.of(14, 0),
                expandedField = null,
            )

        assertFalse(sheet.isConfirmEnabled)
        assertTrue(sheet.copy(endTime = LocalTime.of(15, 0)).isConfirmEnabled)
        assertNull(
            HomeUiState(
                selectedDate = date,
                startTime = LocalTime.of(9, 0),
                endDay = DraftEndDay.SAME_DAY,
                endTime = LocalTime.of(14, 0),
            ).recordDateWindow(zone),
        )
    }

    @Test
    fun `종료는 당일 06시부터 익일 06시까지만 고를 수 있다`() {
        val sheet =
            HomeTimeSheetState(
                recordDate = date,
                startTime = LocalTime.MIDNIGHT,
                endDay = DraftEndDay.SAME_DAY,
                endTime = LocalTime.of(5, 55),
                expandedField = null,
            )

        // 당일 06:00 이전은 최소 길이와 무관하게 범위 밖이다.
        assertFalse(sheet.isConfirmEnabled)
        assertTrue(sheet.copy(endTime = LocalTime.of(6, 0)).isConfirmEnabled)
        // 익일 06:00 이 상한이라 그 뒤는 고를 수 없다.
        assertTrue(sheet.copy(endDay = DraftEndDay.NEXT_DAY, endTime = LocalTime.of(6, 0)).isConfirmEnabled)
        assertFalse(sheet.copy(endDay = DraftEndDay.NEXT_DAY, endTime = LocalTime.of(6, 5)).isConfirmEnabled)
    }

    @Test
    fun `기본 설정인 당일 자정부터 익일 자정까지는 유효하다`() {
        assertNotNull(
            HomeUiState(
                selectedDate = date,
                startTime = LocalTime.MIDNIGHT,
                endDay = DraftEndDay.NEXT_DAY,
                endTime = LocalTime.MIDNIGHT,
            ).recordDateWindow(zone),
        )
    }

    @Test
    fun `시작을 늦추면 종료 하한이 최소 길이만큼 밀린다`() {
        val sheet =
            HomeTimeSheetState(
                recordDate = date,
                startTime = LocalTime.MIDNIGHT,
                endDay = DraftEndDay.NEXT_DAY,
                endTime = LocalTime.MIDNIGHT,
                expandedField = null,
            )

        val late = sheet.withStartTime(LocalTime.of(23, 55))

        assertEquals(date.plusDays(1).atTime(5, 55), late.endRange.start)
        assertEquals(date.plusDays(1).atTime(6, 0), late.endRange.endInclusive)
        // 범위 밖으로 밀린 종료(익일 00:00)는 가까운 경계로 붙는다.
        assertEquals(date.plusDays(1).atTime(5, 55), late.endDateTime)
        assertTrue(late.isConfirmEnabled)
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
        assertEquals(HomeSourceCount(candidate = 2, sending = 0), state.summary.photo)
        // 미리보기는 **후보 기준**이다 — 카드는 무엇이 모였는지를 보여 주고 고른 수는 본문이 말한다.
        assertEquals(listOf("content://photo/2", "content://photo/1"), state.summary.photoPreviewUris)
        assertEquals(1, state.summary.totalItemCount)
    }

    @Test
    fun `사용자 지정 범위는 기준일과 익일 사진만 반열린 구간으로 필터링한다`() {
        val state =
            HomeUiState(
                selectedDate = date,
                startTime = LocalTime.of(22, 0),
                endDay = DraftEndDay.NEXT_DAY,
                // 최소 6시간 정책을 지키는 범위여야 창이 만들어진다.
                endTime = LocalTime.of(4, 0),
            )
        val candidates =
            listOf(
                candidate(id = 1L, dateTime = date.atTime(21, 59)),
                candidate(id = 2L, dateTime = date.atTime(23, 0)),
                candidate(id = 3L, dateTime = date.plusDays(1).atTime(1, 0)),
                candidate(id = 4L, dateTime = date.plusDays(1).atTime(4, 0)),
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

        assertEquals(HomeSourceCount(candidate = 2, sending = 1), selected.summary.photo)
        assertEquals(2, selected.summary.totalItemCount)
        // 고른 것은 2L 하나지만 격자에는 후보 둘이 다 뜬다.
        assertEquals(listOf("content://photo/2", "content://photo/1"), selected.summary.photoPreviewUris)
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

    @Test
    fun `일정 목록은 시작 시각순이고 같은 시각이면 rawId 로 고정한다`() {
        // 카드가 3초마다 넘기며 읽으므로 정렬이 흔들리면 순번이 튄다.
        val items =
            listOf(
                item("cal-b", date.atTime(9, 0), CalendarPayload("나중", null, null, false)),
                item("cal-a", date.atTime(9, 0), CalendarPayload("같은 시각", null, null, false)),
                item("cal-c", date.atTime(8, 0), CalendarPayload("가장 이른", null, null, true)),
            )

        val summary = HomeUiState(selectedDate = date).refreshSourceSummary(items, emptyList(), zone).summary

        assertEquals(listOf("cal-c", "cal-a", "cal-b"), summary.calendarItems.map(HomeCalendarItem::rawId))
        assertEquals(true, summary.calendarItems.first().allDay)
    }

    @Test
    fun `알림은 앱별로 묶어 건수 내림차순으로 두고 표시명은 가장 최근 것을 쓴다`() {
        val items =
            listOf(
                item("n1", date.atTime(8, 0), notification("옛 이름", "com.toss")),
                item("n2", date.atTime(9, 0), notification("토스", "com.toss")),
                item("n3", date.atTime(10, 0), notification("카카오톡", "com.kakao")),
            )

        val apps = HomeUiState(selectedDate = date).refreshSourceSummary(items, emptyList(), zone).summary.notificationApps

        assertEquals(listOf("com.toss", "com.kakao"), apps.map(HomeNotificationApp::packageName))
        assertEquals(listOf(2, 1), apps.map(HomeNotificationApp::count))
        // 앱 이름이 바뀌었을 수 있어 가장 최근 알림의 수집 당시 이름을 쓴다.
        assertEquals("토스", apps.first().appName)
    }

    @Test
    fun `전송 예정 수는 선택 정책 결과에서 나오고 없으면 0 이다`() {
        val items =
            listOf(
                item("cal-1", date.atTime(9, 0), CalendarPayload("일정", null, null, false)),
                item("cal-2", date.atTime(10, 0), CalendarPayload("일정", null, null, false)),
            )
        val state = HomeUiState(selectedDate = date)
        val window = state.recordDateWindow(zone)!!
        val selection = DraftSourceItemSelectionPolicy().select(window, items).getOrThrow()

        val counted = state.refreshSourceSummary(items, emptyList(), zone, selection).summary.calendar
        // 후보 수로 대신 채우면 실제보다 많이 보낸다고 말하게 된다.
        val uncounted = state.refreshSourceSummary(items, emptyList(), zone).summary.calendar

        assertEquals(HomeSourceCount(candidate = 2, sending = 2), counted)
        assertEquals(HomeSourceCount(candidate = 2, sending = 0), uncounted)
    }

    @Test
    fun `사용자가 제외한 항목은 전송 예정 수에서 빠진다`() {
        val items =
            listOf(
                item("cal-1", date.atTime(9, 0), CalendarPayload("일정", null, null, false)),
                item("cal-2", date.atTime(10, 0), CalendarPayload("일정", null, null, false)),
            )
        val state = HomeUiState(selectedDate = date)
        val selection = DraftSourceItemSelectionPolicy().select(state.recordDateWindow(zone)!!, items).getOrThrow()

        val summary = state.refreshSourceSummary(items, emptyList(), zone, selection, setOf("cal-2")).summary

        assertEquals(HomeSourceCount(candidate = 2, sending = 1), summary.calendar)
    }

    @Test
    fun `가장 오래 머문 곳은 전체 체류가 아니라 기록 창과 겹친 시간으로 고른다`() {
        // 창 포함 판정은 구간이 겹치면 참이라, 전날부터 이어진 긴 체류가 오늘 창에 잠깐만
        // 걸쳐도 후보가 된다. 전체 길이로 재면 그것이 오늘 오래 머문 곳을 이긴다.
        val overnight =
            stay(
                id = "overnight",
                start = date.minusDays(1).atTime(18, 0),
                end = date.atTime(0, 10),
                city = "밤샘시",
            )
        val today =
            stay(
                id = "today",
                start = date.atTime(13, 0),
                end = date.atTime(16, 0),
                city = "서울특별시",
            )

        val place = HomeUiState(selectedDate = date).refreshSourceSummary(listOf(overnight, today), emptyList(), zone).summary.stayPlace

        assertEquals("today", place?.rawId)
    }

    @Test
    fun `체류가 없으면 가장 오래 머문 곳도 없다`() {
        val items = listOf(item("cal", date.atTime(9, 0), CalendarPayload("일정", null, null, false)))

        val place = HomeUiState(selectedDate = date).refreshSourceSummary(items, emptyList(), zone).summary.stayPlace

        assertNull(place)
    }

    @Test
    fun `층위가 있으면 두 층으로 적고 없으면 한 줄 주소를 쓴다`() {
        val layered = HomeStayPlace("a", 37.5, 126.9, city = "오산시", district = "원동", line = "대한민국 경기도 오산시 원동 123")
        val lineOnly = HomeStayPlace("b", 37.5, 126.9, line = "대한민국 서울특별시 강남구 역삼동 823")
        val nothing = HomeStayPlace("c", 37.5, 126.9)

        assertEquals("오산시 원동", layered.label)
        // 한 줄로 물러설 때도 나라 이름은 떼고 보여 준다.
        assertEquals("서울특별시 강남구 역삼동 823", lineOnly.label)
        assertNull(nothing.label)
        // 한 줄만 저장된 기존 항목을 해석 완료로 보면 층위가 영영 안 채워진다.
        assertEquals(false, layered.needsResolution)
        assertEquals(true, lineOnly.needsResolution)
    }

    @Test
    fun `두 층위가 같은 이름이면 한 번만 적는다`() {
        // `locality` 와 `subLocality` 에 똑같이 `마포구` 가 실려 저장된 항목이 있다.
        val duplicated = HomeStayPlace("a", 37.5, 126.9, city = "마포구", district = "마포구")

        assertEquals("마포구", duplicated.label)
    }

    @Test
    fun `광역 이름만 저장된 예전 값은 다시 해석한다`() {
        // 층위를 시·군·구로 내리기 전에 저장된 값이다. 완료로 보면 `서울특별시` 한 마디가 남는다.
        val province = HomeStayPlace("a", 37.5, 126.9, city = "서울특별시", line = "대한민국 서울특별시 강남구")
        val province2 = HomeStayPlace("b", 37.5, 126.9, city = "경기도", line = "대한민국 경기도 오산시")

        assertEquals(true, province.needsResolution)
        assertEquals(true, province2.needsResolution)
        // 시·군·구는 광역과 꼬리말이 겹치지 않는다.
        assertEquals(false, HomeStayPlace("c", 37.5, 126.9, city = "오산시").needsResolution)
        assertEquals(false, HomeStayPlace("d", 37.5, 126.9, city = "강남구").needsResolution)
        assertEquals(false, HomeStayPlace("e", 37.5, 126.9, city = "울릉군").needsResolution)
    }

    private fun stay(
        id: String,
        start: LocalDateTime,
        end: LocalDateTime,
        city: String? = null,
    ): SourceItem =
        SourceItem(
            rawId = id,
            startAt = start.atZone(zone).toInstant(),
            endAt = end.atZone(zone).toInstant(),
            timeZoneId = zone,
            payload = StayPayload(latitude = 37.5, longitude = 126.9, addressCity = city),
            sourceName = SourceName.LOCATION_PROVIDER,
            sourceKey = id,
            collectedAt = start.atZone(zone).toInstant(),
        )

    private fun notification(
        appName: String,
        packageName: String,
    ) = NotificationPayload(
        appName = appName,
        packageName = packageName,
        title = "제목",
        text = "본문",
        collectReason = NotificationPayload.CollectReason.KEYWORD,
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
