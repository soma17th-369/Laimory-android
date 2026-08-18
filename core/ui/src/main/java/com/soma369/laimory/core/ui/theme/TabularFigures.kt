package com.soma369.laimory.core.ui.theme

import androidx.compose.ui.text.TextStyle

/**
 * 숫자를 고정폭으로 그린다.
 *
 * 기본 숫자는 글자마다 폭이 달라 `11`과 `08`의 너비가 다르다. 값이 계속 바뀌는 롤러나 시각 표시에서는
 * 글자가 좌우로 흔들려 보이므로 폭을 고정한다.
 *
 * 고정폭 글꼴을 따로 들이지 않고 Pretendard가 이미 가진 OpenType `tnum`(tabular figures)을 켜는
 * 방식이라 한글 모양과 자간은 그대로 유지된다.
 */
fun TextStyle.tabularFigures(): TextStyle = copy(fontFeatureSettings = TABULAR_FIGURES)

private const val TABULAR_FIGURES = "tnum"
