package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class TimelineEventResponse(
    val timelineEventId: Long,
    val eventType: String,
    val startAt: String,
    val endAt: String? = null,
    val title: String,
    val subtitle: String? = null,
    val memo: String? = null,
    val items: List<TimelineItemResponse>,
)

internal fun TimelineEventResponse.toDomain(): TimelineEvent =
    TimelineEvent(
        timelineEventId = timelineEventId,
        eventType = TimelineEventType.entries.firstOrNull { it.name == eventType } ?: TimelineEventType.UNKNOWN,
        startAt = startAt.parseLocalDateTime("startAt"),
        endAt = endAt?.parseLocalDateTime("endAt"),
        title = title,
        subtitle = subtitle,
        memo = memo,
        items = items.map(TimelineItemResponse::toDomain),
    )

internal fun String.parseLocalDateTime(fieldName: String): LocalDateTime =
    runCatching { LocalDateTime.parse(this) }
        .getOrElse { throw ApiException.UnknownException("잘못된 $fieldName 형식입니다: $this") }
