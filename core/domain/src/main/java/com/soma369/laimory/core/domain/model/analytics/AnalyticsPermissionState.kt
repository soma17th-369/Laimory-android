package com.soma369.laimory.core.domain.model.analytics

/** 권한 요청 뒤 확인한 상태. */
enum class AnalyticsPermissionState {
    GRANTED,
    PARTIAL,
    DENIED,
    SETTINGS_REQUIRED,
    UNAVAILABLE,
}
