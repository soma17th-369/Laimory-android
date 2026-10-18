package com.soma369.laimory.core.domain.model.analytics

/** 생성 준비를 시작했지만 요청까지 가지 않은 이유. */
enum class AnalyticsCreateStopReason {
    NO_DATA,
    ALL_EXCLUDED,

    /** 보낼 것은 있었지만 서버 생성 가능 확인이 너무 적다고 판정했다. */
    INSUFFICIENT_DATA,
    CANCELLED,
}
