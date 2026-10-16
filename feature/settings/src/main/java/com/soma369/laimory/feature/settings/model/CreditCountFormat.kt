package com.soma369.laimory.feature.settings.model

import java.text.NumberFormat
import java.util.Locale

/**
 * 크레딧 개수 표시. 1,000 부터 천 단위 쉼표를 찍는다 — 자릿수가 늘어도 한눈에 읽히게.
 *
 * 숫자 칸은 늘어난 자릿수만큼 넓어진다. 자르거나 줄여 쓰지 않는다 — 남은 크레딧은 정확히 보여야 하는 값이다.
 */
internal fun Int.toCreditCount(): String = NumberFormat.getIntegerInstance(Locale.KOREA).format(this)
