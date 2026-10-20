package com.soma369.laimory.core.domain.model.timeline

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
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
    fun `종일 일정은 그 날짜 자정 구간으로 06시 시작 창과의 겹침만으로 포함이 정해진다`() {
        // 수집 시 기기 시간대 자정으로 바꾼 15일 종일 일정: 15일 00:00 ~ 16일 00:00 (#504).
        val day = LocalDate.of(2026, 10, 15)
        val allDay =
            ranged(
                start = at(0, 0, day),
                end = at(0, 0, day.plusDays(1)),
                payload = CalendarPayload("휴가", null, null, allDay = true),
            )

        fun windowFrom(start: LocalDate) = RecordDateWindow(start = at(6, 0, start), end = at(6, 0, start.plusDays(1)))

        assertTrue("14일 06:00 ~ 15일 06:00 과 6시간 겹친다", windowFrom(day.minusDays(1)).contains(allDay))
        assertTrue("15일 06:00 ~ 16일 06:00 과 18시간 겹친다", windowFrom(day).contains(allDay))
        assertFalse("16일 06:00 ~ 17일 06:00 과는 겹치지 않는다", windowFrom(day.plusDays(1)).contains(allDay))
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

    @Test
    fun `종료가 시작과 같거나 앞선 창은 만들 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) { RecordDateWindow(at(9), at(9)) }
        assertThrows(IllegalArgumentException::class.java) { RecordDateWindow(at(10), at(9)) }
    }

    @Test
    fun `사용자 창은 익일 종료까지 포함할 수 있다`() {
        val custom = RecordDateWindow(start = at(9), end = at(2, day = date.plusDays(1)))

        assertTrue(custom.contains(point(at(1, day = date.plusDays(1)))))
        assertFalse(custom.contains(point(at(3, day = date.plusDays(1)))))
    }
}
