package com.soma369.laimory.core.domain.model.timeline

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import java.time.LocalDate
import java.time.ZoneId

/**
 * 종일 일정을 어느 기록 날짜에 넣을지 정하는 규칙.
 *
 * 종일 일정은 사용자에게 "그 하루"다. 그래서 기록 창과의 겹침([RecordDateWindow.contains])이 아니라 **날짜**로 넣는다 —
 * 기록 창이 14일 06:00 ~ 15일 06:00 이어도 15일 종일 일정은 14일 기록에 넣지 않는다. 여러 날 종일 일정은 걸친
 * 날짜마다 들어간다.
 *
 * 시간을 직접 지정한 일정(00:00 ~ 익일 00:00 으로 입력한 것 포함)은 이 규칙을 받지 않고 겹침으로만 거른다.
 * 일정이 아닌 아이템도 그대로 통과한다.
 */
object CalendarAllDayRecordPolicy {
    /**
     * [item] 이 [recordDate] 기록에 들어갈 수 있는지. 종일 일정이면 [recordDate] 가 일정의 날짜 구간
     * `[시작일, 끝일)` 안에 있어야 한다. 날짜는 [zone] 기준이다 — 조회가 종일 일정을 이미 그 시간대 자정으로 바꿔 둔다.
     */
    fun belongsTo(
        item: SourceItem,
        recordDate: LocalDate,
        zone: ZoneId,
    ): Boolean {
        val payload = item.payload
        if (payload !is CalendarPayload || !payload.allDay) return true
        val startDate = item.startAt.atZone(zone).toLocalDate()
        val endDate = item.endAt?.atZone(zone)?.toLocalDate() ?: startDate.plusDays(1)
        return recordDate >= startDate && recordDate < endDate
    }
}
