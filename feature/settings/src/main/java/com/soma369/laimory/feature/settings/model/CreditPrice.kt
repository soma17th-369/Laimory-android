package com.soma369.laimory.feature.settings.model

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/** 원화 가격 표시. 천 단위 쉼표를 찍는다(`3,500원`). */
internal fun Int.toWon(): String = "${NumberFormat.getIntegerInstance(Locale.KOREA).format(this)}원"

/**
 * 충전 상품의 개당 가격(`개당 약 143원`). 묶음이 얼마나 이득인지 견주라고 기간 옆에 붙인다.
 *
 * 1개 상품은 그 자체가 비교 기준이라 붙이지 않는다(null). 나누어떨어지지 않으면 반올림하고 `약` 을 붙인다.
 */
internal fun unitPriceLabel(
    amount: Int,
    price: Int,
): String? {
    if (amount <= 1) return null
    val unit = (price.toDouble() / amount).roundToInt().toWon()
    return if (price % amount == 0) "개당 $unit" else "개당 약 $unit"
}
