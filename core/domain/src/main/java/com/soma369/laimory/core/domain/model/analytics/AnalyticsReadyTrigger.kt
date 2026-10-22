package com.soma369.laimory.core.domain.model.analytics

/** 생성할 재료가 처음 생긴 계기. 사진·일정은 저장하지 않고 볼 때마다 읽어 권한 허용이 곧 준비다. */
enum class AnalyticsReadyTrigger {
    STORED_ITEM,
    PHOTO_PERMISSION,
    CALENDAR_PERMISSION,
}
