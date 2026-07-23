package com.soma369.laimory.core.domain.model.timeline

import java.time.Instant
import java.time.LocalDate

/** 앱이 추적 중인 서버 초안 작업의 최소 복구 정보. */
data class ActiveDraftTask(
    val taskId: String,
    val recordDate: LocalDate,
    val requestedAt: Instant,
) {
    init {
        require(taskId.isNotBlank()) { "taskId는 비어 있을 수 없습니다." }
    }
}
