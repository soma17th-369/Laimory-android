package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDateTime

/**
 * 서버의 통합 Event PATCH 명령.
 *
 * title·subtitle·startAt·endAt은 항상 전송하고, 나머지는 서버 계약에 맞춰 키 존재 여부를 구분한다.
 */
data class UpdateTimelineEventCommand(
    val timelineEventId: Long,
    val title: String,
    /** 빈 문자열은 **지움**, null 은 **유지**다 — 서버 PATCH 계약. 지우려고 null 을 보내면 안 바뀐다. */
    val subtitle: String?,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    val eventType: TimelineEventType? = null,
    val memo: TimelineEventUpdateField<String?> = TimelineEventUpdateField.Unchanged,
    val photosToAdd: TimelineEventUpdateField<List<TimelineEventPhotoAddition>> = TimelineEventUpdateField.Unchanged,
)
