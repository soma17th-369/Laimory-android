package com.soma369.laimory.core.domain.model.analytics

/** 업데이트 안내의 종류. 하한선 미만이면 강제, 권장선 미만이면 권장이다. */
enum class AnalyticsUpdatePromptType {
    FORCED,
    RECOMMENDED,
}
