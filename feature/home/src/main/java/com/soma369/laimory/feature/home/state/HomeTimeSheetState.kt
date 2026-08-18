package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import java.time.LocalTime

/**
 * 초안 범위의 시작·종료를 한 시트에서 고르는 동안의 임시 값. null 이면 시트가 닫힌 상태다.
 *
 * 시트는 상태를 갖지 않으므로 확인 전까지의 값은 화면이 들고 있는다 — 취소하면 이 값이 버려지고
 * 원래 범위가 그대로 남는다.
 *
 * 시작은 기록 날짜에 고정이라 날짜를 고르지 않는다. 종료만 [endDay]로 당일·익일을 옮긴다.
 */
@Immutable
data class HomeTimeSheetState(
    val startTime: LocalTime,
    val endDay: DraftEndDay,
    val endTime: LocalTime,
    val expandedField: HomeTimeField?,
) {
    /**
     * 종료가 시작보다 뒤여야 기록 창이 성립한다.
     *
     * 종료가 익일이면 시각과 무관하게 항상 뒤이므로 당일일 때만 시각을 비교한다.
     */
    val isConfirmEnabled: Boolean
        get() = endDay == DraftEndDay.NEXT_DAY || endTime > startTime
}
