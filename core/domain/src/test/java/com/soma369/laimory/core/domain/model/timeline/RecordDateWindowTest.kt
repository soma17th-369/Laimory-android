package com.soma369.laimory.core.domain.model.timeline

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class RecordDateWindowTest {
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val date: LocalDate = LocalDate.of(2026, 7, 8)
    private val window = RecordDateWindow.ofDate(date, zone)

    /** [day] 로컬 시각을 UTC instant 로. */
    private fun at(
        hour: Int,
        minute: Int = 0,
        day: LocalDate = date,
    ): Instant = day.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant()

    private fun ranged(
        start: Instant,
        end: Instant,
        payload: SourceItemPayload = HealthPayload(HealthPayload.Metric.SLEEP, 0.0, "minute"),
    ): SourceItem = item(start = start, end = end, payload = payload)

    private fun point(start: Instant): SourceItem =
        item(start = start, end = null, payload = PhotoPayload("f.jpg", "content://f", null, null, null))

    private fun item(
        start: Instant,
        end: Instant?,
        payload: SourceItemPayload,
    ): SourceItem =
        SourceItem(
            rawId = "raw-$start",
            startAt = start,
            endAt = end,
            timeZoneId = zone,
            payload = payload,
            sourceName = SourceName.HEALTH_CONNECT,
            sourceKey = "key-$start",
            collectedAt = start,
        )

    @Test
    fun `ofDate 는 그날 자정부터 다음날 자정까지`() {
        assertEquals(at(0, 0), window.start)
        assertEquals(at(0, 0, date.plusDays(1)), window.end)
    }

    @Test
    fun `창 안의 단일 시점 이벤트는 포함`() {
        assertTrue(window.contains(point(at(13, 0))))
    }

    @Test
    fun `창 시작 자정 시점은 포함, 다음날 자정 시점은 제외`() {
        assertTrue("창 시작(00:00)은 그날에 포함", window.contains(point(at(0, 0))))
        assertFalse("다음날 00:00 은 다음날 것", window.contains(point(at(0, 0, date.plusDays(1)))))
    }

    @Test
    fun `아침에 걸친 수면은 그날로 딸려온다`() {
        val overnight = ranged(start = at(23, 0, date.minusDays(1)), end = at(7, 0))
        assertTrue(window.contains(overnight))
    }

    @Test
    fun `밤에 걸친 수면은 그날로 딸려온다`() {
        val overnight = ranged(start = at(23, 30), end = at(6, 30, date.plusDays(1)))
        assertTrue(window.contains(overnight))
    }

    @Test
    fun `자정을 걸친 일정도 그날로 딸려온다`() {
        val event =
            ranged(
                start = at(22, 0),
                end = at(1, 0, date.plusDays(1)),
                payload = CalendarPayload("야근", null, null, allDay = false),
            )
        assertTrue(window.contains(event))
    }

    @Test
    fun `창을 완전히 벗어난 아이템은 제외`() {
        val yesterday = point(at(10, 0, date.minusDays(1)))
        val tomorrow = ranged(start = at(9, 0, date.plusDays(1)), end = at(10, 0, date.plusDays(1)))
        assertFalse(window.contains(yesterday))
        assertFalse(window.contains(tomorrow))
    }

    @Test
    fun `양끝을 모두 넘는 종일 수면도 포함`() {
        // 전날 밤부터 다음날 아침까지 창 전체를 덮는 세션.
        val allDay = ranged(start = at(20, 0, date.minusDays(1)), end = at(8, 0, date.plusDays(1)))
        assertTrue(Duration.between(allDay.startAt, allDay.endAt).toHours() > 24)
        assertTrue(window.contains(allDay))
    }

    @Test
    fun `창 시작에 맞닿아 끝나는 구간은 그날 창에서 제외`() {
        // 어제 걸음수 day-bucket `[어제 00:00, 오늘 00:00)` 은 어제 것 — 오늘 창에 이중 계수되면 안 된다.
        val yesterdayBucket = ranged(start = at(0, 0, date.minusDays(1)), end = at(0, 0))
        assertFalse(window.contains(yesterdayBucket))

        // 전날 23:00~오늘 00:00 구간도 끝이 창 시작에 맞닿을 뿐이라 제외.
        val endsAtStart = ranged(start = at(23, 0, date.minusDays(1)), end = at(0, 0))
        assertFalse(window.contains(endsAtStart))
    }

    @Test
    fun `오늘 day-bucket 구간은 오늘 창에 포함`() {
        val todayBucket = ranged(start = at(0, 0), end = at(0, 0, date.plusDays(1)))
        assertTrue(window.contains(todayBucket))
    }
}
