package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.settings.DefaultRecordRange
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
    /**
     * `기본값으로 지정` 을 사용자가 직접 누른 값. null 이면 아직 누르지 않았다.
     *
     * 누르기 전에는 **지금 범위가 저장된 기본값과 같은지**를 그대로 보여 준다([isSavingAsDefault]). 처음에
     * 체크돼 있던 것을 고정해 두면, 기본값 그대로 열어 이번만 범위를 바꾼 사람이 확인하는 순간 기본값까지
     * 덮어쓴다. 직접 누른 뒤로는 범위를 바꿔도 그 선택을 따른다.
     */
    val saveAsDefault: Boolean? = null,
) {
    val range: DefaultRecordRange
        get() = defaultRecordRangeOf(startTime, endDay, endTime)

    /** 확인할 때 이 범위를 기본값으로 저장할지. 체크박스가 보여 주는 값이기도 하다. */
    fun isSavingAsDefault(defaultRange: DefaultRecordRange): Boolean = saveAsDefault ?: (range == defaultRange)

    /**
     * 확인하면 기본값이 될 범위. `기본값으로 지정` 아래 줄이 보여 준다.
     *
     * 체크돼 있으면 지금 고른 범위가 곧 새 기본값이라 그것을, 아니면 저장된 기본값을 그대로 보여 준다. 그래야
     * 스피너로 범위를 바꾸고 체크하는 순간 아래 줄이 함께 바뀌어, 무엇이 기본값이 될지 확인 전에 보인다.
     */
    fun defaultAfterConfirm(defaultRange: DefaultRecordRange): DefaultRecordRange =
        if (isSavingAsDefault(defaultRange)) range else defaultRange

    fun hasRangeOf(
        startTime: LocalTime,
        endDay: DraftEndDay,
        endTime: LocalTime,
    ): Boolean = this.startTime == startTime && this.endDay == endDay && this.endTime == endTime
}
