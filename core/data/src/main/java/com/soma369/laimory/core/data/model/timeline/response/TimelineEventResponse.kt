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
    /** AI 가 Event 마다 생성해 되묻는 문장. 서버 편집 API 로 바꿀 수 없는 읽기 전용 값이다. */
    val question: String? = null,
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
        question = question,
        items = items.map(TimelineItemResponse::toDomain),
    )

internal fun String.parseLocalDateTime(fieldName: String): LocalDateTime =
    runCatching { LocalDateTime.parse(this) }
        .getOrElse { throw ApiException.UnknownException("잘못된 $fieldName 형식입니다: $this") }
