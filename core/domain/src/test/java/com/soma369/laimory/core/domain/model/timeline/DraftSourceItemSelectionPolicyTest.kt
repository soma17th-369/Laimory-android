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
    fun `기본 상한은 권장 Maximum을 따른다`() {
        val limits = DraftSourceItemLimits()

        assertEquals(30, limits.stay)
        assertEquals(30, limits.movement)
        assertEquals(20, limits.calendar)
        assertEquals(30, limits.health)
        assertEquals(100, limits.notification)
        assertEquals(20, limits.photo)
    }

    @Test
    fun `기록 창 필터 후 타입별 최신 항목만 선택한다`() {
        val policy = DraftSourceItemSelectionPolicy(limits = limits(notification = 2))
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
    fun `타입별 상한은 독립적으로 적용되고 전체 상한으로 잘라내지 않는다`() {
        val policy =
            DraftSourceItemSelectionPolicy(
                limits =
                    limits(
                        stay = 1,
                        notification = 2,
                        photo = 2,
                    ),
            )
        val photo1 = item("photo-1", ItemType.PHOTO, minute = 1)
        val photo2 = item("photo-2", ItemType.PHOTO, minute = 2)
        val oldStay = item("stay-old", ItemType.STAY, minute = 3)
        val newStay = item("stay-new", ItemType.STAY, minute = 4)
        val notification1 = item("notification-1", ItemType.NOTIFICATION, minute = 5)
        val notification2 = item("notification-2", ItemType.NOTIFICATION, minute = 6)

        val selection =
            policy
                .select(window, listOf(notification1, photo2, oldStay, newStay, notification2, photo1))
                .getOrThrow()

        // STAY만 타입 상한(1)으로 잘리고, 나머지는 타입 상한 합이 몇이든 전부 전송된다.
        assertEquals(listOf(photo1, photo2, newStay, notification1, notification2), selection.items)
        assertEquals(6, selection.report.originalTotal)
        assertEquals(5, selection.report.selectedTotal)
        assertEquals(1, selection.report.excludedCounts.getValue(ItemType.STAY))
        assertEquals(0, selection.report.excludedCounts.getValue(ItemType.NOTIFICATION))
    }

    @Test
    fun `동일 시각이면 rawId 오름차순으로 안정 선택하고 전송한다`() {
        val policy = DraftSourceItemSelectionPolicy(limits = limits(notification = 2))
        val rawC = item("c", ItemType.NOTIFICATION, minute = 1)
        val rawA = item("a", ItemType.NOTIFICATION, minute = 1)
        val rawB = item("b", ItemType.NOTIFICATION, minute = 1)

        val selection = policy.select(window, listOf(rawC, rawA, rawB)).getOrThrow()

        assertEquals(listOf(rawA, rawB), selection.items)
    }

    @Test
    fun `구간 데이터의 최신 기준은 endAt이 아니라 startAt이다`() {
        val policy = DraftSourceItemSelectionPolicy(limits = limits(movement = 1))
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
        val policy = DraftSourceItemSelectionPolicy(limits = limits(photo = 2))
        val photos = (1..3).map { index -> item("photo-$index", ItemType.PHOTO, minute = index) }

        val result = policy.select(window, photos)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as DraftPhotoLimitExceededException
        assertEquals(3, exception.selectedCount)
        assertEquals(2, exception.maxCount)
    }

    @Test
    fun `기록 창 밖 PHOTO가 상한을 넘어도 창 안 PHOTO만으로 판정한다`() {
        val outsidePhotos =
            (1..21).map { index ->
                item("outside-photo-$index", ItemType.PHOTO, minute = -index)
            }
        val insidePhotos =
            (1..5).map { index ->
                item("inside-photo-$index", ItemType.PHOTO, minute = index)
            }

        val selection =
            DraftSourceItemSelectionPolicy()
                .select(window, outsidePhotos + insidePhotos)
                .getOrThrow()

        assertEquals(insidePhotos, selection.items)
        assertEquals(5, selection.report.originalCounts.getValue(ItemType.PHOTO))
    }

    @Test
    fun `최종 목록은 startAt 오름차순 이후 rawId 오름차순으로 정렬한다`() {
        val policy = DraftSourceItemSelectionPolicy(limits = limits())
        val late = item("late", ItemType.CALENDAR, minute = 3)
        val sameTimeB = item("b", ItemType.HEALTH, minute = 2)
        val early = item("early", ItemType.STAY, minute = 1)
        val sameTimeA = item("a", ItemType.NOTIFICATION, minute = 2)

        val selection = policy.select(window, listOf(late, sameTimeB, early, sameTimeA)).getOrThrow()

        assertEquals(listOf(early, sameTimeA, sameTimeB, late), selection.items)
    }

    @Test
    fun `excluding 은 제외 항목만 빼고 상한 여유를 재충원하지 않는다`() {
        val policy = DraftSourceItemSelectionPolicy(limits = limits(notification = 2))
        val selection =
            policy.select(
                window,
                listOf(
                    item("noti-1", ItemType.NOTIFICATION, minute = 1),
                    item("noti-2", ItemType.NOTIFICATION, minute = 2),
                    item("noti-3", ItemType.NOTIFICATION, minute = 3),
                ),
            ).getOrThrow()
        assertEquals(listOf("noti-2", "noti-3"), selection.items.map(SourceItem::rawId))

        val submission = selection.excluding(setOf("noti-3"))

        // 상한 때문에 잘렸던 noti-1 이 빈자리로 다시 들어오지 않는다.
        assertEquals(listOf("noti-2"), submission.items.map(SourceItem::rawId))
        assertEquals(1, submission.report.selectedCounts[ItemType.NOTIFICATION])
        // 원본 건수는 수집 기준 그대로 유지된다.
        assertEquals(3, submission.report.originalCounts[ItemType.NOTIFICATION])
    }

    @Test
    fun `excluding 은 PHOTO 제외 요청을 무시한다`() {
        val selection =
            DraftSourceItemSelectionPolicy()
                .select(
                    window,
                    listOf(
                        item("photo-1", ItemType.PHOTO, minute = 1),
                        item("cal-1", ItemType.CALENDAR, minute = 2),
                    ),
                ).getOrThrow()

        val submission = selection.excluding(setOf("photo-1", "cal-1"))

        // 사진은 홈 선택이 정본 — 잘못 전달된 사진 rawId 는 items 와 selectedCounts 에 그대로 남는다.
        assertEquals(listOf("photo-1"), submission.items.map(SourceItem::rawId))
        assertEquals(1, submission.report.selectedCounts[ItemType.PHOTO])
        assertEquals(0, submission.report.selectedCounts[ItemType.CALENDAR])
    }

    @Test
    fun `excluding 에 빈 집합을 주면 같은 선택을 반환한다`() {
        val selection =
            DraftSourceItemSelectionPolicy()
                .select(window, listOf(item("cal-1", ItemType.CALENDAR, minute = 1)))
                .getOrThrow()

        assertEquals(selection, selection.excluding(emptySet()))
    }

    // --- 수집 사유 우선순위 (#253) ---

    @Test
    fun `상한을 넘으면 클릭 알림 최소 스무 건을 보장한다`() {
        val policy = DraftSourceItemSelectionPolicy()
        val keywords = notifications("kw", 200, NotificationPayload.CollectReason.KEYWORD, fromMinute = 1)
        val clicks = notifications("click", 100, NotificationPayload.CollectReason.CLICK, fromMinute = 201)

        val selected = policy.select(window, keywords + clicks).getOrThrow().items

        assertEquals(100, selected.size)
        assertEquals(20, selected.count { it.reason == NotificationPayload.CollectReason.CLICK })
        assertEquals(80, selected.count { it.reason == NotificationPayload.CollectReason.KEYWORD })
    }

    @Test
    fun `클릭 알림이 쿼터보다 적으면 남은 자리를 다른 사유가 채운다`() {
        val policy = DraftSourceItemSelectionPolicy(limits = limits(notification = 10))
        val keywords = notifications("kw", 20, NotificationPayload.CollectReason.KEYWORD, fromMinute = 1)
        val clicks = notifications("click", 3, NotificationPayload.CollectReason.CLICK, fromMinute = 21)

        val selected = policy.select(window, keywords + clicks).getOrThrow().items

        assertEquals(10, selected.size)
        assertEquals(3, selected.count { it.reason == NotificationPayload.CollectReason.CLICK })
        assertEquals(7, selected.count { it.reason == NotificationPayload.CollectReason.KEYWORD })
    }

    @Test
    fun `쿼터를 채우고 남은 클릭 알림도 우선순위 경쟁에 남는다`() {
        // 쿼터 20건을 채운 뒤 키워드가 자리를 다 못 쓰면 나머지 클릭이 이어서 들어온다.
        val policy = DraftSourceItemSelectionPolicy(limits = limits(notification = 25))
        val keywords = notifications("kw", 2, NotificationPayload.CollectReason.KEYWORD, fromMinute = 1)
        val clicks = notifications("click", 30, NotificationPayload.CollectReason.CLICK, fromMinute = 11)

        val selected = policy.select(window, keywords + clicks).getOrThrow().items

        assertEquals(25, selected.size)
        assertEquals(2, selected.count { it.reason == NotificationPayload.CollectReason.KEYWORD })
        // 쿼터 20 + 남은 자리 3
        assertEquals(23, selected.count { it.reason == NotificationPayload.CollectReason.CLICK })
    }

    @Test
    fun `사유 우선순위는 키워드 클릭 앱 전체 순이다`() {
        val policy = DraftSourceItemSelectionPolicy(limits = limits(notification = 3))
        val all = item("all", ItemType.NOTIFICATION, minute = 4, reason = NotificationPayload.CollectReason.ALL)
        val app = item("app", ItemType.NOTIFICATION, minute = 3, reason = NotificationPayload.CollectReason.APP)
        val click = item("click", ItemType.NOTIFICATION, minute = 2, reason = NotificationPayload.CollectReason.CLICK)
        val keyword = item("kw", ItemType.NOTIFICATION, minute = 1, reason = NotificationPayload.CollectReason.KEYWORD)

        val selected = policy.select(window, listOf(all, app, click, keyword)).getOrThrow().items

        // 최신순으로는 ALL 이 먼저지만 사유 우선순위가 앞선다. 클릭은 쿼터로도 보장된다.
        assertEquals(setOf("kw", "click", "app"), selected.mapTo(mutableSetOf(), SourceItem::rawId))
    }

    @Test
    fun `같은 사유 안에서는 최신 항목이 우선한다`() {
        val policy = DraftSourceItemSelectionPolicy(limits = limits(notification = 2))
        val oldest = item("oldest", ItemType.NOTIFICATION, minute = 1, reason = NotificationPayload.CollectReason.KEYWORD)
        val middle = item("middle", ItemType.NOTIFICATION, minute = 2, reason = NotificationPayload.CollectReason.KEYWORD)
        val newest = item("newest", ItemType.NOTIFICATION, minute = 3, reason = NotificationPayload.CollectReason.KEYWORD)

        val selected = policy.select(window, listOf(oldest, newest, middle)).getOrThrow().items

        assertEquals(listOf(middle, newest), selected)
    }

    @Test
    fun `상한 이하이면 사유와 무관하게 최신순 결과를 유지한다`() {
        val policy = DraftSourceItemSelectionPolicy(limits = limits(notification = 5))
        val all = item("all", ItemType.NOTIFICATION, minute = 1, reason = NotificationPayload.CollectReason.ALL)
        val keyword = item("kw", ItemType.NOTIFICATION, minute = 2, reason = NotificationPayload.CollectReason.KEYWORD)
        val click = item("click", ItemType.NOTIFICATION, minute = 3, reason = NotificationPayload.CollectReason.CLICK)

        val selected = policy.select(window, listOf(click, all, keyword)).getOrThrow().items

        // 절삭이 없으면 우선순위는 의미가 없다. 최종 정렬은 기존대로 오래된 순이다.
        assertEquals(listOf(all, keyword, click), selected)
    }

    @Test
    fun `리포트에 알림 사유별 원본과 선택 건수를 남긴다`() {
        val policy = DraftSourceItemSelectionPolicy(limits = limits(notification = 4))
        val keywords = notifications("kw", 6, NotificationPayload.CollectReason.KEYWORD, fromMinute = 1)
        val clicks = notifications("click", 2, NotificationPayload.CollectReason.CLICK, fromMinute = 11)

        val report = policy.select(window, keywords + clicks).getOrThrow().report

        assertEquals(6, report.notificationOriginalCountsByReason.getValue(NotificationPayload.CollectReason.KEYWORD))
        assertEquals(2, report.notificationOriginalCountsByReason.getValue(NotificationPayload.CollectReason.CLICK))
        assertEquals(2, report.notificationSelectedCountsByReason.getValue(NotificationPayload.CollectReason.KEYWORD))
        assertEquals(2, report.notificationSelectedCountsByReason.getValue(NotificationPayload.CollectReason.CLICK))
        assertEquals(0, report.notificationOriginalCountsByReason.getValue(NotificationPayload.CollectReason.APP))
    }

    @Test
    fun `동의 화면 제외 후에도 사유별 선택 건수를 갱신한다`() {
        val policy = DraftSourceItemSelectionPolicy(limits = limits(notification = 4))
        val keyword = item("kw", ItemType.NOTIFICATION, minute = 1, reason = NotificationPayload.CollectReason.KEYWORD)
        val click = item("click", ItemType.NOTIFICATION, minute = 2, reason = NotificationPayload.CollectReason.CLICK)

        val remaining = policy.select(window, listOf(keyword, click)).getOrThrow().excluding(setOf("click"))

        // 원본 건수는 수집 기준 그대로 두고 선택분만 줄인다.
        assertEquals(1, remaining.report.notificationOriginalCountsByReason.getValue(NotificationPayload.CollectReason.CLICK))
        assertEquals(0, remaining.report.notificationSelectedCountsByReason.getValue(NotificationPayload.CollectReason.CLICK))
        assertEquals(1, remaining.report.notificationSelectedCountsByReason.getValue(NotificationPayload.CollectReason.KEYWORD))
    }

    @Test
    fun `알림 사유 우선순위는 다른 타입 선택에 영향을 주지 않는다`() {
        val policy = DraftSourceItemSelectionPolicy(limits = limits(notification = 1, stay = 2))
        val stays = (1..4).map { item("stay-$it", ItemType.STAY, minute = it) }
        val notifications = notifications("kw", 3, NotificationPayload.CollectReason.KEYWORD, fromMinute = 11)

        val selected = policy.select(window, stays + notifications).getOrThrow().items

        assertEquals(listOf("stay-3", "stay-4"), selected.filter { it.itemType == ItemType.STAY }.map(SourceItem::rawId))
    }

    private fun notifications(
        prefix: String,
        count: Int,
        reason: NotificationPayload.CollectReason,
        fromMinute: Int,
    ): List<SourceItem> =
        (0 until count).map { index ->
            item("$prefix-$index", ItemType.NOTIFICATION, minute = fromMinute + index, reason = reason)
        }

    private val SourceItem.reason: NotificationPayload.CollectReason?
        get() = (payload as? NotificationPayload)?.collectReason

    private fun limits(
        stay: Int = 10,
        movement: Int = 10,
        calendar: Int = 10,
        health: Int = 10,
        notification: Int = 10,
        photo: Int = 10,
    ): DraftSourceItemLimits =
        DraftSourceItemLimits(
            stay = stay,
            movement = movement,
            calendar = calendar,
            health = health,
            notification = notification,
            photo = photo,
        )

    private fun item(
        rawId: String,
        itemType: ItemType,
        minute: Int,
        endMinute: Int? = null,
        reason: NotificationPayload.CollectReason = NotificationPayload.CollectReason.APP,
    ): SourceItem =
        SourceItem(
            rawId = rawId,
            startAt = window.start.plusSeconds(minute * 60L),
            endAt = endMinute?.let { window.start.plusSeconds(it * 60L) },
            timeZoneId = java.time.ZoneId.of("UTC"),
            payload = payload(itemType, reason),
            sourceName = SourceName.NOTIFICATION_LISTENER,
            sourceKey = "key-$rawId",
            collectedAt = window.start.plusSeconds(minute * 60L),
        )

    private fun payload(
        itemType: ItemType,
        reason: NotificationPayload.CollectReason = NotificationPayload.CollectReason.APP,
    ): SourceItemPayload =
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
                    collectReason = reason,
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
