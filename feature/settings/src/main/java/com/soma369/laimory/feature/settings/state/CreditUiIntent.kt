package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface CreditUiIntent : UiIntent {
    /** 화면 진입·복귀. 잔액은 만들기로 줄어드는 값이라 들어올 때마다 다시 받는다. */
    data object Refresh : CreditUiIntent

    data object NavigateBack : CreditUiIntent

    /**
     * 충전 상품 한 줄을 눌렀다. 충전은 아직 열지 않아 안내만 한다.
     *
     * @param amount 누른 상품의 크레딧 개수. 수요 측정 이벤트에 그대로 싣는다.
     */
    data class PackageClicked(
        val amount: Int,
    ) : CreditUiIntent
}
