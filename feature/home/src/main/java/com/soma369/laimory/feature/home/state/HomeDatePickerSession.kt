package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.settings.DefaultRecordRange
import java.time.LocalDate
import java.time.LocalTime

/**
 * 날짜 피커가 열려 있는 동안의 임시 값. null 이면 피커가 닫힌 상태다.
 *
 * 날짜와 기록 범위를 **함께** 확정한다. 그래서 둘의 임시 값을 여기 모은다 — 피커의 확인은 날짜·범위를 확정하고
 * 피커를 닫는다. 시간 시트의 확인(`확인` · `이 날만` · `항상 이 시간으로`)도 날짜·범위를 곧바로 확정하되 시트만 닫고
 * 피커는 열어 둔다. 그 뒤 피커를 취소해도 시트에서 확정한 것은 되돌리지 않는다.
 *
 * 범위는 홈 카드가 보여 줄 데이터를 거르는 필터다. 기록이 있는 날도 막지 않는다 — 그날 CTA 는
 * 이미 있는 기록을 열 뿐이라 범위가 기록에 닿지 않는다.
 */
@Immutable
data class HomeDatePickerSession(
    val date: LocalDate,
    val startTime: LocalTime,
    val endDay: DraftEndDay,
    val endTime: LocalTime,
) {
    val range: DefaultRecordRange
        get() = defaultRecordRangeOf(startTime, endDay, endTime)

    fun hasRangeOf(
        startTime: LocalTime,
        endDay: DraftEndDay,
        endTime: LocalTime,
    ): Boolean = this.startTime == startTime && this.endDay == endDay && this.endTime == endTime
}
