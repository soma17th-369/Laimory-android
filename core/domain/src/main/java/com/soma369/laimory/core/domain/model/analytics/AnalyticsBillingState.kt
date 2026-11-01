package com.soma369.laimory.core.domain.model.analytics

/** 충전 상품을 눌렀을 때 결제를 열 수 있었는지. */
enum class AnalyticsBillingState {
    /** 충전 준비 중이라 안내만 했다. */
    COMING_SOON,

    /** 결제 창을 열었다. */
    AVAILABLE,
}
