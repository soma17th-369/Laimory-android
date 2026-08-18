package com.soma369.laimory.feature.collection.state.sleep

import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalDate
import java.time.LocalTime

sealed interface SleepInputUiIntent : UiIntent {
    /** 시각 선택 시트를 열고 누른 줄을 펼친다. */
    data class ShowTimePicker(val field: SleepTimeField) : SleepInputUiIntent

    /** 시트 안에서 펼친 줄을 바꾼다. null 이면 모두 접는다. */
    data class ExpandTimeField(val field: SleepTimeField?) : SleepInputUiIntent

    /** 시트 롤러를 굴린 결과. 확인 전까지는 시트 임시 값만 바뀐다. */
    data class ChangeSheetTime(
        val field: SleepTimeField,
        val time: LocalTime,
    ) : SleepInputUiIntent

    /** 시트의 확인 — 임시 값을 취침·기상 시각으로 확정한다. */
    data object ConfirmTimeSheet : SleepInputUiIntent

    data object DismissTimePicker : SleepInputUiIntent

    data class SetBedTime(val time: LocalTime) : SleepInputUiIntent

    data class SetWakeTime(val time: LocalTime) : SleepInputUiIntent

    /** 기록 대상 밤(기상일)을 바꾼다. 미래는 허용하지 않는다(오늘까지). */
    data class SelectDate(val date: LocalDate) : SleepInputUiIntent

    data object PreviousDay : SleepInputUiIntent

    data object NextDay : SleepInputUiIntent

    data object ShowDatePicker : SleepInputUiIntent

    data object DismissDatePicker : SleepInputUiIntent

    /** 수면 자동 감지를 켜거나 끈다(켜기 전 활동 인식·HC 쓰기 권한 확보는 화면이 선행). */
    data class SetAutoDetection(val enabled: Boolean) : SleepInputUiIntent

    /** 입력한 수면을 Health Connect 에 저장한다(권한 확보는 화면이 선행). */
    data object Save : SleepInputUiIntent
}
