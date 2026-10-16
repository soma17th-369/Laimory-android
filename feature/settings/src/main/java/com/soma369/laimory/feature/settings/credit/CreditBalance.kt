package com.soma369.laimory.feature.settings.credit

/**
 * 기억해 둔 마지막 크레딧. 아직 받지 않았거나 못 받았으면 `null` 이다(0 으로 두면 남은 게 없는 줄 안다).
 *
 * @property remaining 남은 크레딧.
 * @property cost 타임라인 한 번 만들기에 드는 크레딧. 크레딧 화면만 받는다.
 */
data class CreditBalance(
    val remaining: Int? = null,
    val cost: Int? = null,
)
