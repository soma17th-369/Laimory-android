package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 초안 범위의 시작·종료를 한 시트에서 고르는 동안의 임시 값. null 이면 시트가 닫힌 상태다.
 *
 * 시트는 상태를 갖지 않으므로 확인 전까지의 값은 화면이 들고 있는다 — 취소하면 이 값이 버려지고
 * 원래 범위가 그대로 남는다.
 *
 * 시작은 기록 날짜에 고정이라 날짜를 고르지 않는다. 종료만 [endDay]로 당일·익일을 옮기며,
 * 고를 수 있는 폭은 [DraftWindowPolicy]가 정한다.
 */
@Immutable
data class HomeTimeSheetState(
    val recordDate: LocalDate,
    val startTime: LocalTime,
    val endDay: DraftEndDay,
    val endTime: LocalTime,
    val expandedField: HomeTimeField?,
) {
    val startDateTime: LocalDateTime
        get() = recordDate.atTime(startTime)

    val endDateTime: LocalDateTime
        get() = recordDate.plusDays(endDay.dayOffset.toLong()).atTime(endTime)

    /** 종료로 고를 수 있는 범위. 시작이 늦어질수록 최소 길이만큼 함께 밀린다. */
    val endRange: ClosedRange<LocalDateTime>
        get() = DraftWindowPolicy.endRange(recordDate, startTime)

    val isConfirmEnabled: Boolean
        get() = DraftWindowPolicy.isValid(recordDate, startTime, endDateTime)

    /** 시작을 옮겨 종료가 범위 밖으로 밀리면 가까운 경계로 붙인다. */
    fun withStartTime(startTime: LocalTime): HomeTimeSheetState = copy(startTime = startTime).withEndCoercedIntoRange()

    fun withEnd(endDateTime: LocalDateTime): HomeTimeSheetState =
        copy(
            endDay = if (endDateTime.toLocalDate() == recordDate) DraftEndDay.SAME_DAY else DraftEndDay.NEXT_DAY,
            endTime = endDateTime.toLocalTime(),
        )

    private fun withEndCoercedIntoRange(): HomeTimeSheetState = withEnd(DraftWindowPolicy.coerceEnd(recordDate, startTime, endDateTime))
}
