package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalTime

/**
 * 날짜 피커가 열려 있는 동안의 임시 값. null 이면 피커가 닫힌 상태다.
 *
 * 날짜와 기록 범위를 **한 다이얼로그의 확인으로 함께** 확정한다. 그래서 둘의 임시 값을 여기 모은다 —
 * 시간 시트의 확인은 이 세션만 바꾸고, 다이얼로그를 취소하면 날짜·범위가 모두 버려진다. 날짜는
 * 화면이, 범위는 홈 상태가 따로 들고 있으면 시트에서 확인한 범위가 다이얼로그 취소 뒤에도 남는다.
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
    fun hasRangeOf(
        startTime: LocalTime,
        endDay: DraftEndDay,
        endTime: LocalTime,
    ): Boolean = this.startTime == startTime && this.endDay == endDay && this.endTime == endTime
}
