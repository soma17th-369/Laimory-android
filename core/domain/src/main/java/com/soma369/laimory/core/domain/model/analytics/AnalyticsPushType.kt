package com.soma369.laimory.core.domain.model.analytics

/** 눌러 연 알림의 종류. */
enum class AnalyticsPushType {
    TIMELINE_COMPLETION_SUCCESS,
    TIMELINE_COMPLETION_FAILED,
    DAILY_REMINDER,

    /** 알림 값은 실려 왔지만 위 어느 것과도 맞지 않는다. */
    UNKNOWN,
}
