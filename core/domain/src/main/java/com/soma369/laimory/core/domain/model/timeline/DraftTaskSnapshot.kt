package com.soma369.laimory.core.domain.model.timeline

/**
 * 초안 생성 작업 상태 스냅샷(`GET /timeline/drafts/{taskId}`).
 *
 * 상태마다 사용하는 필드가 다르다.
 * - [DraftTaskStatus.PROCESSING]: optional [elapsedSeconds]
 * - [DraftTaskStatus.SUCCESS]: [result]
 * - [DraftTaskStatus.FAILED]: [failure]
 */
data class DraftTaskSnapshot(
    val status: DraftTaskStatus,
    val result: DailyTimeline? = null,
    val failure: DraftTaskFailureReason? = null,
    val elapsedSeconds: Long? = null,
)
