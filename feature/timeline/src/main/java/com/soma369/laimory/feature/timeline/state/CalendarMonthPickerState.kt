package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable

/**
 * TopBar 월 표기를 눌러 연·월을 고르는 피커의 상태. null 이면 닫힌 상태다.
 *
 * [year] 는 피커 안에서 넘겨보는 연도로, 월을 고르기 전까지는 표시 월을 바꾸지 않는다.
 * 연도만 넘기다 그냥 닫으면 달력은 원래 월에 그대로 있어야 하기 때문이다.
 */
@Immutable
data class CalendarMonthPickerState(
    val year: Int,
)
