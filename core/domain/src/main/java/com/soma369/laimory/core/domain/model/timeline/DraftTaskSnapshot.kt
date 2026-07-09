package com.soma369.laimory.core.domain.model.timeline

/**
 * 초안 생성 작업 상태 스냅샷(`GET /timeline/drafts/{taskId}`).
 *
 * 생성 흐름의 완료 여부 확인용이다 — [status] 가 [DraftTaskStatus.FAILED] 이면 [error] 에 사유가 담긴다.
 * 성공 시 실제 타임라인 결과(events·서버 enrich payload)는 표시 기능에서 별도로 다룬다.
 */
data class DraftTaskSnapshot(
    val status: DraftTaskStatus,
    val error: String?,
)
