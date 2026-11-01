package com.soma369.laimory.core.domain.model.analytics

/** 일일 리마인더 문구의 종류. */
enum class AnalyticsReminderKind {
    /** 기록을 권하는 문구. */
    GENERAL,

    /** 그날 기록을 이미 마친 날의 문구. */
    COMPLETED,
}
