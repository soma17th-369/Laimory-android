package com.soma369.laimory.feature.collection.state.sleep

import androidx.compose.runtime.Immutable
import java.time.LocalTime

/**
 * 취침·기상을 한 시트에서 고르는 동안의 임시 값. null 이면 시트가 닫힌 상태다.
 *
 * 시트는 상태를 갖지 않으므로 확인 전까지의 값은 화면이 들고 있는다 — 취소하면 이 값이 버려지고
 * 원래 취침·기상 시각이 그대로 남는다.
 *
 * 날짜는 다루지 않는다. 취침일은 `bedTime >= wakeTime` 이면 기상일 전날로 파생되는 계산값이라
 * 사용자가 따로 고를 값이 아니다.
 */
@Immutable
data class SleepTimeSheetState(
    val bedTime: LocalTime,
    val wakeTime: LocalTime,
    val expandedField: SleepTimeField?,
) {
    fun withTime(
        field: SleepTimeField,
        time: LocalTime,
    ): SleepTimeSheetState =
        when (field) {
            SleepTimeField.BED -> copy(bedTime = time)
            SleepTimeField.WAKE -> copy(wakeTime = time)
        }
}
