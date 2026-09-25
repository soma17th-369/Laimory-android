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
 */
@Immutable
data class HomeDatePickerSession(
    val date: LocalDate,
    val startTime: LocalTime,
    val endDay: DraftEndDay,
    val endTime: LocalTime,
    val rangeLock: HomeRangeLock,
    /**
     * 범위를 바꾼 채 확인을 눌렀는데 판정이 아직이라 확정을 미루는 중.
     *
     * 판정이 도착하면 확인을 다시 적용한다. 그동안 확인·날짜·범위 입력은 받지 않고 취소만 받는다.
     */
    val isConfirmPending: Boolean = false,
) {
    /** 범위 칩을 누를 수 있는지. 판정 중·조회 실패에도 열어 두고, 바뀐 범위는 확인에서 거른다. */
    val isRangeEditable: Boolean
        get() = rangeLock != HomeRangeLock.LOCKED && !isConfirmPending

    fun hasRangeOf(
        startTime: LocalTime,
        endDay: DraftEndDay,
        endTime: LocalTime,
    ): Boolean = this.startTime == startTime && this.endDay == endDay && this.endTime == endTime
}
