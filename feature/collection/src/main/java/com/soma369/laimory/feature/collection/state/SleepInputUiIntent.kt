package com.soma369.laimory.feature.collection.state

import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalTime

sealed interface SleepInputUiIntent : UiIntent {
    data class ShowTimePicker(val field: SleepTimeField) : SleepInputUiIntent

    data object DismissTimePicker : SleepInputUiIntent

    data class SetBedTime(val time: LocalTime) : SleepInputUiIntent

    data class SetWakeTime(val time: LocalTime) : SleepInputUiIntent

    /** 입력한 수면을 Health Connect 에 저장한다(권한 확보는 화면이 선행). */
    data object Save : SleepInputUiIntent
}
