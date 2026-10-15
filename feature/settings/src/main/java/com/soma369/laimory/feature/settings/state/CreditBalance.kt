package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.credit.TimelineCredit

/**
 * 크레딧 화면 잔액 카드의 상태.
 *
 * 받는 중과 못 받음을 나눈다 — 받는 중에 `-` 를 찍으면 곧 숫자로 바뀌어 깜빡이고, 못 받았는데 0 을 찍으면 남은 게
 * 없는 줄 안다.
 */
@Immutable
sealed interface CreditBalance {
    data object Loading : CreditBalance

    /** 잔액·비용 중 하나라도 못 받았다. 숫자 자리는 `-`, 쓰임 줄은 숨긴다. */
    data object Unavailable : CreditBalance

    data class Loaded(
        val credit: TimelineCredit,
    ) : CreditBalance
}
