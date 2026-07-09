package com.soma369.laimory.core.domain.model.timeline

/** 초안 생성 작업의 진행 상태. 서버 `status` 값과 1:1 매핑된다. */
enum class DraftTaskStatus {
    PROCESSING,
    SUCCESS,
    FAILED,
}
