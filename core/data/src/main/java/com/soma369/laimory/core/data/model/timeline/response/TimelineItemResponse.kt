package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.model.timeline.TimelineItem
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TimelineItemResponse(
    val timelineItemId: Long,
    val itemType: String,
    val rawId: String,
    val startAt: String,
    val endAt: String? = null,
    val payload: JsonElement,
)

internal fun TimelineItemResponse.toDomain(): TimelineItem =
    TimelineItem(
        timelineItemId = timelineItemId,
        itemType = TimelineItemType.entries.firstOrNull { it.name == itemType } ?: TimelineItemType.UNKNOWN,
        rawId = rawId,
        startAt = startAt.parseLocalDateTime("startAt"),
        endAt = endAt?.parseLocalDateTime("endAt"),
    )
