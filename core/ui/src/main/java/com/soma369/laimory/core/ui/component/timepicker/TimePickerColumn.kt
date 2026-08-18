package com.soma369.laimory.core.ui.component.timepicker

/**
 * 사용자가 실제로 굴린 롤러 열.
 *
 * 화면이 값을 보정할 때 어느 단위로 되돌릴지 정하는 데 쓴다 — 시를 굴리다 규칙을 어겼으면 시 단위로,
 * 분을 굴리다 어겼으면 분 단위로 맞춰야 사용자가 방금 만진 열이 크게 튀지 않는다.
 */
enum class TimePickerColumn {
    DATE,
    HOUR,
    MINUTE,
}
