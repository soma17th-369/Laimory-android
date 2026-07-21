package com.soma369.laimory.core.domain.model.timeline

/** 폴링 FAILED 응답을 사용자 복구 정책에 필요한 의미로 변환한 실패 분류. */
enum class DraftTaskFailureReason {
    AI_REPORTED_FAILURE,
    AI_DISPATCH_FAILURE,
    STAGING_DATA_MISSING,
    FINALIZE_FAILURE,
    UNKNOWN,
}
