package com.soma369.laimory.core.domain.model.timeline

/** 폴링 API의 정상 응답과 복구 가능한 404 계약을 함께 표현한다. */
sealed interface DraftTaskStatusOutcome {
    data class Snapshot(
        val value: DraftTaskSnapshot,
    ) : DraftTaskStatusOutcome

    /** `-1001`: 작업이 없거나 만료됐거나 다른 사용자의 작업이다. */
    data object TaskUnavailable : DraftTaskStatusOutcome

    /** `-404`: SUCCESS 결과 기록이 삭제됐거나 결과 ID가 없는 legacy 작업이다. */
    data object ResultUnavailable : DraftTaskStatusOutcome
}
