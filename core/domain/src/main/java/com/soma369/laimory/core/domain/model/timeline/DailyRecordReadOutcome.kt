package com.soma369.laimory.core.domain.model.timeline

/** 단건 조회 API의 정상 응답과 복구 가능한 `-404` 계약을 함께 표현한다. */
sealed interface DailyRecordReadOutcome {
    data class Record(
        val value: DailyTimeline,
    ) : DailyRecordReadOutcome

    /** `-404`: 기록이 존재하지 않거나 다른 사용자의 기록이다. */
    data object Unavailable : DailyRecordReadOutcome
}
