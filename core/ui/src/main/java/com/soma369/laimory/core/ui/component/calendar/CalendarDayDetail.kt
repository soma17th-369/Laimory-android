package com.soma369.laimory.core.ui.component.calendar

import androidx.compose.runtime.Immutable

/**
 * 셀 하나에 호출부가 붙이는 말.
 *
 * 격자는 날짜·오늘 여부·고를 수 있는지까지만 안다. `기록 있음, 감정 기쁨` 처럼 그 날에 무엇이
 * 있는지는 격자를 쓰는 화면이 안다.
 *
 * @param note 낭독 문구 끝에 덧붙일 말. 없으면 날짜만 읽는다.
 * @param clickLabel 탭이 무엇을 하는지 — `기록 열기` / `날짜 선택`. 없으면 시스템 기본 문구다.
 */
@Immutable
data class CalendarDayDetail(
    val note: String? = null,
    val clickLabel: String? = null,
)
