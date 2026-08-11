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
    ): Instant = date.atTime(hour, minute).atZone(zone).toInstant()

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
        val window = RecordDateWindow.ofDate(date, zone)
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
        assertEquals("3건 전송", location.countLabel)
        assertEquals(listOf("체류", "이동"), location.sections.map { it.title })
        assertEquals(2, location.sections[0].items.size)
        assertEquals(1, location.sections[1].items.size)
        // 주소가 있으면 주소를, 없으면 전송 좌표를 제목으로 보여준다.
        assertEquals("서울시 강남구", location.sections[0].items[0].title)
        assertTrue(location.sections[0].items[1].title.contains("위도"))
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
        assertEquals("0건 · 전송되지 않음", photo.countLabel)
        assertTrue(photo.sections.isEmpty())
    }

    @Test
    fun `사진 미리보기는 스냅샷의 사진 URI 를 최대 3장까지 제공한다`() {
        val photos =
            (1..5).map { index ->
                item(
                    "photo-$index",
                    at(10, index),
                    PhotoPayload("$index.jpg", "content://photo/$index", 37.5, 127.0, null),
                )
            }

        val content = preparation(photos).toConsentContent()

        assertEquals(3, content.photoPreviewUris.size)
        val photoSummary = content.typeSummaries.first { it.group == DraftConsentTypeGroup.PHOTO }
        assertEquals(5, photoSummary.sentCount)
        assertEquals("촬영 위치(EXIF) 포함", photoSummary.sections.single().items.first().description)
    }

    @Test
    fun `전체 전송 건수는 선택 리포트 합계와 같다`() {
        val calendar = item("cal-1", at(9), CalendarPayload("회의", "설명", null, false))
        val photo = item("photo-1", at(10), PhotoPayload("1.jpg", "content://photo/1", null, null, null))

        val content = preparation(listOf(calendar, photo)).toConsentContent()

        assertEquals(2, content.sentTotal)
    }
}
