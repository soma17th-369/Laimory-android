package com.soma369.laimory.feature.collection.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState
import java.time.LocalDate
import java.time.LocalTime

@Immutable
data class SleepInputUiState(
    /** 기록할 밤(기상일 기준). 기본은 어제. */
    val wakeDate: LocalDate = LocalDate.now(),
    val bedTime: LocalTime = LocalTime.of(23, 0),
    val wakeTime: LocalTime = LocalTime.of(7, 0),
    /** 이 밤에 이미(우리·외부) 수면 기록이 있는지. */
    val alreadyRecorded: Boolean = false,
    val isSaving: Boolean = false,
    /** 시간 피커 다이얼로그 대상. null 이면 닫힘. */
    val editingField: SleepTimeField? = null,
) : UiState
