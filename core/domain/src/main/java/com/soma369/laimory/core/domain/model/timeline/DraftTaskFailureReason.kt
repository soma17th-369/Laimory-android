package com.soma369.laimory.core.domain.model.timeline

/** 폴링 FAILED 응답을 사용자 복구 정책에 필요한 의미로 변환한 실패 분류. */
enum class DraftTaskFailureReason {
    AI_REPORTED_FAILURE,
    AI_DISPATCH_FAILURE,

    /**
     * 그날 기록에서 타임라인에 넣을 활동을 하나도 찾지 못했다(`-1022`).
     *
     * 장애가 아니라 입력이 모자란 경우라, 같은 기록으로 다시 만들어도 결과가 같다. 재시도를 권하지 않는다.
     */
    NO_EVENTS,
    FINALIZE_FAILURE,
    UNKNOWN,
}
