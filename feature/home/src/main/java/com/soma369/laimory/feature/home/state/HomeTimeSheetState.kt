package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.settings.DefaultRecordRange
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
    /**
     * 사용자가 고른 종료. null 이면 아직 종료를 건드리지 않아 시트를 연 때의 종료가 기준이다.
     *
     * 시작이 늦어지면 최소 길이 때문에 종료가 밀리는데, 밀린 값을 그대로 다음 기준으로 쓰면 시작을 되돌려도 종료가
     * 돌아오지 않는다. 롤러는 굴리는 도중에도 값을 내서, 시(時)가 순환하며 늦은 시각을 한 번 지나기만 해도 고른 적
     * 없는 종료(예: 익일 05:45)가 남는다. 그래서 밀기는 늘 이 값에서 다시 계산한다.
     */
    val pickedEnd: LocalDateTime? = null,
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

    /** 고르고 있는 범위. 저장된 기본값과 다르면 시트가 `항상 이 시간으로`·`이 날만` 을 함께 묻는다. */
    val range: DefaultRecordRange
        get() = defaultRecordRangeOf(startTime, endDay, endTime)

    /**
     * 시작을 옮긴다. 고른 종료([pickedEnd])가 새 범위 밖이면 가까운 경계로 붙이고, 범위 안으로 돌아오면 고른 종료로
     * 되돌린다.
     */
    fun withStartTime(startTime: LocalTime): HomeTimeSheetState {
        val picked = pickedEnd ?: endDateTime
        return copy(startTime = startTime, pickedEnd = picked)
            .placeEnd(DraftWindowPolicy.coerceEnd(recordDate, startTime, picked))
    }

    /** 사용자가 종료를 고른다. 이 값이 이후 시작이 바뀔 때 돌아올 기준이 된다. */
    fun withEnd(endDateTime: LocalDateTime): HomeTimeSheetState = copy(pickedEnd = endDateTime).placeEnd(endDateTime)

    private fun placeEnd(endDateTime: LocalDateTime): HomeTimeSheetState =
        copy(
            endDay = if (endDateTime.toLocalDate() == recordDate) DraftEndDay.SAME_DAY else DraftEndDay.NEXT_DAY,
            endTime = endDateTime.toLocalTime(),
        )
}
