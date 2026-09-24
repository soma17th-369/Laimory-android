package com.soma369.laimory.core.domain.model.analytics

/** 완료 확정이 어떻게 이뤄졌는지. 응답 유실 뒤 이미 저장된 상태를 복구한 경우가 RECOVERED 다. */
enum class AnalyticsCompletionOutcome {
    TRANSITIONED,
    RECOVERED,
}
