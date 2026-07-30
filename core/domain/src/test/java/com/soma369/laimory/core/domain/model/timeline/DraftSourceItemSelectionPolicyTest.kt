package com.soma369.laimory.core.domain.model.timeline

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.collection.StayPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DraftSourceItemSelectionPolicyTest {
    private val window =
        RecordDateWindow(
            start = Instant.parse("2026-07-30T00:00:00Z"),
            end = Instant.parse("2026-08-01T00:00:00Z"),
        )

    @Test
    fun `기본 상한은 권장 Maximum과 전체 전송 상한을 따른다`() {
        val limits = DraftSourceItemLimits()

        assertEquals(30, limits.stay)
        assertEquals(30, limits.movement)
        assertEquals(20, limits.calendar)
        assertEquals(30, limits.health)
        assertEquals(100, limits.notification)
        assertEquals(20, limits.photo)
        assertEquals(100, limits.total)
    }

    @Test
    fun `기록 창 필터 후 타입별 최신 항목만 선택한다`() {
        val policy =
            DraftSourceItemSelectionPolicy(
                limits =
                    limits(
                        notification = 2,
                        total = 10,
                    ),
            )
        val outside = item("outside", ItemType.NOTIFICATION, minute = -1)
        val oldest = item("oldest", ItemType.NOTIFICATION, minute = 1)
        val middle = item("middle", ItemType.NOTIFICATION, minute = 2)
        val newest = item("newest", ItemType.NOTIFICATION, minute = 3)

        val selection = policy.select(window, listOf(outside, middle, oldest, newest)).getOrThrow()

        assertEquals(listOf(middle, newest), selection.items)
        assertEquals(3, selection.report.originalCounts.getValue(ItemType.NOTIFICATION))
        assertEquals(2, selection.report.selectedCounts.getValue(ItemType.NOTIFICATION))
        assertEquals(1, selection.report.excludedCounts.getValue(ItemType.NOTIFICATION))
    }

    @Test
    fun `전체 상한에서는 PHOTO를 예약하고 남은 자리를 최신 비사진으로 채운다`() {
        val policy =
            DraftSourceItemSelectionPolicy(
                limits =
                    limits(
                        stay = 3,
                        notification = 3,
                        photo = 2,
                        total = 4,
                    ),
            )
        val photo1 = item("photo-1", ItemType.PHOTO, minute = 1)
        val photo2 = item("photo-2", ItemType.PHOTO, minute = 2)
        val stay = item("stay", ItemType.STAY, minute = 3)
        val notification1 = item("notification-1", ItemType.NOTIFICATION, minute = 4)
        val notification2 = item("notification-2", ItemType.NOTIFICATION, minute = 5)

        val selection =
            policy
                .select(window, listOf(notification1, photo2, stay, notification2, photo1))
                .getOrThrow()

        assertEquals(listOf(photo1, photo2, notification1, notification2), selection.items)
        assertEquals(5, selection.report.originalTotal)
        assertEquals(4, selection.report.selectedTotal)
        assertEquals(1, selection.report.excludedCounts.getValue(ItemType.STAY))
    }

    @Test
    fun `동일 시각이면 rawId 오름차순으로 안정 선택하고 전송한다`() {
        val policy =
            DraftSourceItemSelectionPolicy(
                limits =
                    limits(
                        notification = 2,
                        photo = 2,
                        total = 2,
                    ),
            )
        val rawC = item("c", ItemType.NOTIFICATION, minute = 1)
        val rawA = item("a", ItemType.NOTIFICATION, minute = 1)
        val rawB = item("b", ItemType.NOTIFICATION, minute = 1)

        val selection = policy.select(window, listOf(rawC, rawA, rawB)).getOrThrow()

        assertEquals(listOf(rawA, rawB), selection.items)
    }

    @Test
    fun `구간 데이터의 최신 기준은 endAt이 아니라 startAt이다`() {
        val policy =
            DraftSourceItemSelectionPolicy(
                limits =
                    limits(
                        movement = 1,
                        photo = 1,
                        total = 1,
                    ),
            )
        val earlyStartLateEnd =
            item(
                rawId = "early",
                itemType = ItemType.MOVEMENT,
                minute = 1,
                endMinute = 100,
            )
        val lateStartEarlyEnd =
            item(
                rawId = "late",
                itemType = ItemType.MOVEMENT,
                minute = 2,
                endMinute = 3,
            )

        val selection = policy.select(window, listOf(earlyStartLateEnd, lateStartEarlyEnd)).getOrThrow()

        assertEquals(listOf(lateStartEarlyEnd), selection.items)
    }

    @Test
    fun `PHOTO 상한 초과는 자동 절삭하지 않고 실패한다`() {
        val policy =
            DraftSourceItemSelectionPolicy(
                limits =
                    limits(
                        photo = 2,
                        total = 4,
                    ),
            )
        val photos = (1..3).map { index -> item("photo-$index", ItemType.PHOTO, minute = index) }

        val result = policy.select(window, photos)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as DraftPhotoLimitExceededException
        assertEquals(3, exception.selectedCount)
        assertEquals(2, exception.maxCount)
    }

    @Test
    fun `최종 목록은 startAt 오름차순 이후 rawId 오름차순으로 정렬한다`() {
        val policy = DraftSourceItemSelectionPolicy(limits = limits(total = 10))
        val late = item("late", ItemType.CALENDAR, minute = 3)
        val sameTimeB = item("b", ItemType.HEALTH, minute = 2)
        val early = item("early", ItemType.STAY, minute = 1)
        val sameTimeA = item("a", ItemType.NOTIFICATION, minute = 2)

        val selection = policy.select(window, listOf(late, sameTimeB, early, sameTimeA)).getOrThrow()

        assertEquals(listOf(early, sameTimeA, sameTimeB, late), selection.items)
    }

    private fun limits(
        stay: Int = 10,
        movement: Int = 10,
        calendar: Int = 10,
        health: Int = 10,
        notification: Int = 10,
        photo: Int = 10,
        total: Int,
    ): DraftSourceItemLimits =
        DraftSourceItemLimits(
            stay = stay,
            movement = movement,
            calendar = calendar,
            health = health,
            notification = notification,
            photo = photo,
            total = total,
        )

    private fun item(
        rawId: String,
        itemType: ItemType,
        minute: Int,
        endMinute: Int? = null,
    ): SourceItem =
        SourceItem(
            rawId = rawId,
            startAt = window.start.plusSeconds(minute * 60L),
            endAt = endMinute?.let { window.start.plusSeconds(it * 60L) },
            timeZoneId = java.time.ZoneId.of("UTC"),
            payload = payload(itemType),
            sourceName = SourceName.NOTIFICATION_LISTENER,
            sourceKey = "key-$rawId",
            collectedAt = window.start.plusSeconds(minute * 60L),
        )

    private fun payload(itemType: ItemType): SourceItemPayload =
        when (itemType) {
            ItemType.STAY -> StayPayload(latitude = 37.0, longitude = 127.0)
            ItemType.MOVEMENT ->
                MovementPayload(
                    start = GeoPoint(latitude = 37.0, longitude = 127.0),
                    end = GeoPoint(latitude = 37.1, longitude = 127.1),
                    distanceMeters = 10.0,
                    transports = MovementPayload.Transport.WALKING,
                )
            ItemType.CALENDAR -> CalendarPayload(title = "일정", description = null, locationText = null, allDay = false)
            ItemType.HEALTH -> HealthPayload(metric = HealthPayload.Metric.STEPS, value = 100.0, unit = "count")
            ItemType.NOTIFICATION ->
                NotificationPayload(
                    appName = "앱",
                    packageName = "com.example",
                    title = "제목",
                    text = "본문",
                    collectReason = NotificationPayload.CollectReason.APP,
                )
            ItemType.PHOTO ->
                PhotoPayload(
                    fileName = "$itemType.jpg",
                    clientPhotoUri = "content://$itemType",
                    latitude = null,
                    longitude = null,
                    description = null,
                )
        }
}
