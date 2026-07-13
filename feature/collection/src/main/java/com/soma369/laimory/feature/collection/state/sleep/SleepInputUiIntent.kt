package com.soma369.laimory.feature.collection.state.sleep

import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalDate
import java.time.LocalTime

sealed interface SleepInputUiIntent : UiIntent {
    data class ShowTimePicker(val field: SleepTimeField) : SleepInputUiIntent

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
