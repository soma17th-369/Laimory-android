package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDateTime

/** AI가 구성한 타임라인의 Event와 소속 Item 목록. */
data class TimelineEvent(
    val timelineEventId: Long,
    val eventType: TimelineEventType,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    val title: String,
    val subtitle: String?,
    val memo: String?,
    val items: List<TimelineItem>,
)
