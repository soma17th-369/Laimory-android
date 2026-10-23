package com.soma369.laimory.core.domain.model.timeline

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.collection.StayPayload
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CalendarAllDayRecordPolicyTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val oct14 = LocalDate.of(2026, 10, 14)
    private val oct15 = LocalDate.of(2026, 10, 15)

    @Test
    fun `다음 날 종일 일정은 창과 겹쳐도 그날 기록에 넣지 않는다`() {
        val allDay15 = allDay(oct15, days = 1)

        assertFalse(CalendarAllDayRecordPolicy.belongsTo(allDay15, oct14, zone))
        assertTrue(CalendarAllDayRecordPolicy.belongsTo(allDay15, oct15, zone))
    }

    @Test
    fun `여러 날 종일 일정은 걸친 날짜마다 넣고 끝일은 뺀다`() {
        val allDay14to15 = allDay(oct14, days = 2)

        assertFalse(CalendarAllDayRecordPolicy.belongsTo(allDay14to15, oct14.minusDays(1), zone))
        assertTrue(CalendarAllDayRecordPolicy.belongsTo(allDay14to15, oct14, zone))
        assertTrue(CalendarAllDayRecordPolicy.belongsTo(allDay14to15, oct15, zone))
        assertFalse(CalendarAllDayRecordPolicy.belongsTo(allDay14to15, oct15.plusDays(1), zone))
    }

    @Test
    fun `00시부터 익일 00시로 직접 입력한 일정은 날짜로 거르지 않는다`() {
        val timed =
            item(
                start = oct15.atStartOfDay(),
                end = oct15.plusDays(1).atStartOfDay(),
                payload = CalendarPayload(title = "야간 근무", description = null, locationText = null, allDay = false),
            )

        assertTrue(CalendarAllDayRecordPolicy.belongsTo(timed, oct14, zone))
    }

    @Test
    fun `일정이 아닌 아이템은 그대로 통과한다`() {
        val stay =
            item(
                start = oct15.atTime(1, 0),
                end = oct15.atTime(2, 0),
                payload = StayPayload(latitude = 37.5, longitude = 127.0),
            )

        assertTrue(CalendarAllDayRecordPolicy.belongsTo(stay, oct14, zone))
    }

    private fun allDay(
        date: LocalDate,
        days: Long,
    ) = item(
        start = date.atStartOfDay(),
        end = date.plusDays(days).atStartOfDay(),
        payload = CalendarPayload(title = "휴가", description = null, locationText = null, allDay = true),
    )

    private fun item(
        start: LocalDateTime,
        end: LocalDateTime?,
        payload: SourceItemPayload,
    ) = SourceItem(
        rawId = "id",
        startAt = start.atZone(zone).toInstant(),
        endAt = end?.atZone(zone)?.toInstant(),
        timeZoneId = zone,
        payload = payload,
        sourceName = SourceName.CALENDAR_PROVIDER,
        sourceKey = "key",
        collectedAt = start.atZone(zone).toInstant(),
    )
}
