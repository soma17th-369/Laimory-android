package com.soma369.laimory.core.ui.component.timepicker

import java.time.LocalTime

/**
 * 시간대 표기. 정시는 `21시`, 분이 있으면 `21시 5분`.
 *
 * 정시만 고르는 피커([TimePickerMinuteStep.HOUR])의 값을 화면에 옮길 때 쓴다. 분이 있는 값은 그 피커가 생기기
 * 전에 저장된 것이라, 내림해 보이면 실제와 다른 시각을 말하게 된다.
 */
fun LocalTime.toHourLabel(): String = if (minute == 0) "${hour}시" else "${hour}시 ${minute}분"
