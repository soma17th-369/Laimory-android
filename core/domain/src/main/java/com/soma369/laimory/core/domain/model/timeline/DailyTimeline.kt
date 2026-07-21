package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDate

/** draft 생성이 완료된 하루 타임라인 기록. */
data class DailyTimeline(
    val dailyRecordId: Long,
    val recordDate: LocalDate,
    val emotion: TimelineEmotion?,
    val events: List<TimelineEvent>,
)
