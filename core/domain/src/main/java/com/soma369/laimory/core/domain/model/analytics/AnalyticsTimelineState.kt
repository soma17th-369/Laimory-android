package com.soma369.laimory.core.domain.model.analytics

import com.soma369.laimory.core.domain.model.timeline.DailyRecordStatus

/** 조회한 타임라인의 상태. 편집 이벤트에서는 `record_state` 로 행동 시점의 기록 상태를 싣는다. */
enum class AnalyticsTimelineState {
    DRAFT,
    SAVED,
    ;

    companion object {
        /** SAVED 로 확인되지 않은 기록(상태를 모르는 응답 포함)은 작성 중이다. */
        fun of(status: DailyRecordStatus?): AnalyticsTimelineState = if (status == DailyRecordStatus.SAVED) SAVED else DRAFT
    }
}
