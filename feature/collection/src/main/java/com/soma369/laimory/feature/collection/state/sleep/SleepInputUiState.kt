package com.soma369.laimory.feature.collection.state.sleep

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
    /** 이 밤에 외부 앱(삼성헬스 등)이 쓴 수면이 있는지. 저장 시 덮어쓰기 불가·중복 경고 판단. */
    val hasExternalRecord: Boolean = false,
    /** 수면 자동 감지(Sleep API → HC) 활성 여부. 온보딩 카드 토글의 소스. */
    val autoDetectionEnabled: Boolean = false,
    val isSaving: Boolean = false,
    /** 시간 피커 다이얼로그 대상. null 이면 닫힘. */
    val editingField: SleepTimeField? = null,
    /** 날짜 선택 다이얼로그 노출 여부. */
    val showDatePicker: Boolean = false,
) : UiState
