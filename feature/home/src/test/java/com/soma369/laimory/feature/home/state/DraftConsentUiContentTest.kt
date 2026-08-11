package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.collection.StayPayload
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemLimits
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.feature.home.draft.DraftConsentPreparation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DraftConsentUiContentTest {
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val date: LocalDate = LocalDate.of(2026, 8, 11)

    private fun at(
        hour: Int,
        minute: Int = 0,
        dayOffset: Long = 0,
    ): Instant = date.plusDays(dayOffset).atTime(hour, minute).atZone(zone).toInstant()

    private fun item(
        rawId: String,
        start: Instant,
        payload: SourceItemPayload,
        end: Instant? = null,
    ): SourceItem =
        SourceItem(
            rawId = rawId,
            startAt = start,
            endAt = end,
            timeZoneId = zone,
            payload = payload,
            sourceName = SourceName.CALENDAR_PROVIDER,
            sourceKey = rawId,
            collectedAt = start,
        )

    private fun preparation(
        items: List<SourceItem>,
        limits: DraftSourceItemLimits = DraftSourceItemLimits(),
    ): DraftConsentPreparation {
        val window =
            RecordDateWindow(
                start = date.atStartOfDay(zone).toInstant(),
                end = date.plusDays(2).atStartOfDay(zone).toInstant(),
            )
        val selection = DraftSourceItemSelectionPolicy(limits).select(window, items).getOrThrow()
        return DraftConsentPreparation(
            attemptId = 1L,
            recordDate = date,
            zone = zone,
            window = window,
            selection = selection,
            discardActiveTask = false,
        )
    }

    @Test
    fun `위치는 체류와 이동을 합산해 표시하고 상세에서 구분한다`() {
        val stay1 = item("stay-1", at(9), StayPayload(37.5, 127.0, address = "서울시 강남구"), end = at(10))
        val stay2 = item("stay-2", at(12), StayPayload(37.6, 127.1), end = at(13))
        val move =
            item(
                "move-1",
                at(10, 30),
                MovementPayload(
                    start = GeoPoint(37.5, 127.0),
                    end = GeoPoint(37.6, 127.1),
                    distanceMeters = 2400.0,
                    transports = MovementPayload.Transport.WALKING,
                ),
                end = at(11),
            )

        val content = preparation(listOf(stay1, stay2, move)).toConsentContent()

        val location = content.typeSummaries.first { it.group == DraftConsentTypeGroup.LOCATION }
        assertEquals(3, location.sentCount)
        assertEquals("3건 포함", location.countLabel)
        assertEquals(listOf("체류한 장소", "이동 기록"), location.sections.map { it.title })
        assertEquals(2, location.sections[0].items.size)
        assertEquals(1, location.sections[1].items.size)
        // 주소가 있으면 주소를, 없으면 전송 좌표를 제목으로 보여준다.
        assertEquals("서울시 강남구", location.sections[0].items[0].title)
        assertTrue(location.sections[0].items[1].title.contains("위도"))
        assertEquals("2.4km · 도보", location.sections[1].items[0].description)
    }

    @Test
    fun `상한 초과 유형은 수집 대비 전송 건수를 표기한다`() {
        val notifications =
            (1..3).map { index ->
                item(
                    "noti-$index",
                    at(10, index),
                    NotificationPayload("앱", "com.app", "제목$index", "본문", NotificationPayload.CollectReason.APP),
                )
            }

        val content =
            preparation(
                items = notifications,
                limits = DraftSourceItemLimits(notification = 2),
            ).toConsentContent()

        val notification = content.typeSummaries.first { it.group == DraftConsentTypeGroup.NOTIFICATION }
        assertEquals(3, notification.originalCount)
        assertEquals(2, notification.sentCount)
        assertEquals("수집 3건 중 2건 전송", notification.countLabel)
        assertEquals(2, notification.sections.single().items.size)
    }

    @Test
    fun `전송 0건 유형은 숨기지 않고 전송되지 않음으로 표시한다`() {
        val calendar = item("cal-1", at(9), CalendarPayload("회의", null, "회의실", false))

        val content = preparation(listOf(calendar)).toConsentContent()

        assertEquals(DraftConsentTypeGroup.entries.size, content.typeSummaries.size)
        val photo = content.typeSummaries.first { it.group == DraftConsentTypeGroup.PHOTO }
        assertFalse(photo.isSent)
        assertEquals("0장 · 전송되지 않음", photo.countLabel)
        assertTrue(photo.sections.isEmpty())
    }

    @Test
    fun `사진 상세는 최신 날짜부터 날짜별 섹션으로 묶는다`() {
        val todayPhoto =
            item("photo-today", at(10), PhotoPayload("today.jpg", "content://photo/today", 37.5, 127.0, null))
        val nextDayPhoto =
            item(
                "photo-next",
                at(1, 0, dayOffset = 1),
                PhotoPayload("next.jpg", "content://photo/next", null, null, null),
            )

        val content = preparation(listOf(todayPhoto, nextDayPhoto)).toConsentContent()

        val photo = content.typeSummaries.first { it.group == DraftConsentTypeGroup.PHOTO }
        assertEquals("2장 포함", photo.countLabel)
        assertEquals(listOf("2026년 8월 12일", "2026년 8월 11일"), photo.sections.map { it.title })
        val todayItem = photo.sections[1].items.single()
        assertEquals("content://photo/today", todayItem.imageUri)
        assertEquals("촬영 위치(EXIF) 포함", todayItem.description)
    }

    @Test
    fun `일정 상세는 날짜별로 묶고 종일 일정을 표기한다`() {
        val meeting = item("cal-1", at(14), CalendarPayload("팀 미팅", null, null, false), end = at(15))
        val allDay = item("cal-2", at(0, 0, dayOffset = 1), CalendarPayload("휴가", null, null, true))

        val content = preparation(listOf(meeting, allDay)).toConsentContent()

        val calendar = content.typeSummaries.first { it.group == DraftConsentTypeGroup.CALENDAR }
        assertEquals(2, calendar.sections.size)
        assertEquals("14:00 ~ 15:00", calendar.sections[0].items.single().timeText)
        assertEquals("종일", calendar.sections[1].items.single().timeText)
    }

    @Test
    fun `알림 상세는 앱별로 묶는다`() {
        val kakao1 =
            item("noti-1", at(10), NotificationPayload("카카오톡", "com.kakao", "민우", "미팅 시간", NotificationPayload.CollectReason.APP))
        val kakao2 =
            item("noti-2", at(11), NotificationPayload("카카오톡", "com.kakao", "지현", "저녁 약속", NotificationPayload.CollectReason.APP))
        val delivery =
            item("noti-3", at(12), NotificationPayload("배달의민족", "com.baemin", null, "배달 완료", NotificationPayload.CollectReason.CLICK))

        val content = preparation(listOf(kakao1, kakao2, delivery)).toConsentContent()

        val notification = content.typeSummaries.first { it.group == DraftConsentTypeGroup.NOTIFICATION }
        assertEquals(listOf("카카오톡", "배달의민족"), notification.sections.map { it.title })
        assertEquals(2, notification.sections[0].items.size)
        assertEquals("(제목 없음)", notification.sections[1].items.single().title)
    }

    @Test
    fun `전체 전송 건수는 선택 리포트 합계와 같다`() {
        val calendar = item("cal-1", at(9), CalendarPayload("회의", "설명", null, false))
        val photo = item("photo-1", at(10), PhotoPayload("1.jpg", "content://photo/1", null, null, null))

        val content = preparation(listOf(calendar, photo)).toConsentContent()

        assertEquals(2, content.sentTotal)
    }
}
