package com.soma369.laimory.core.domain.model.timeline

/**
 * 서버 초안 생성 작업 핸들. `POST /timeline/drafts` 는 비동기(202)라 즉시 결과 대신
 * 작업 식별자([taskId])만 돌려준다. 실제 타임라인 결과는 [taskId] 로 폴링해서 받는다.
 */
data class DraftTaskHandle(
    val taskId: String,
)
