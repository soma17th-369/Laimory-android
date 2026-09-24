package com.soma369.laimory.core.domain.model.analytics

/** 생성 준비를 시작했지만 요청까지 가지 않은 이유. */
enum class AnalyticsCreateStopReason {
    NO_DATA,
    ALL_EXCLUDED,
    CANCELLED,
}
