package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDateTime

/** Event를 구성하는 수집 데이터의 공통 메타데이터. 타입별 raw payload는 Data 계층에 머문다. */
data class TimelineItem(
    val timelineItemId: Long,
    val itemType: TimelineItemType,
    val rawId: String,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    val photoUrl: String? = null,
)
