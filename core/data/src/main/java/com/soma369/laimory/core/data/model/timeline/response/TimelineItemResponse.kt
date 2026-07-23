package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.model.timeline.TimelineItem
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class TimelineItemResponse(
    val timelineItemId: Long,
    val itemType: String,
    val rawId: String,
    val startAt: String? = null,
    val endAt: String? = null,
    val payload: JsonElement,
)

internal fun TimelineItemResponse.toDomain(): TimelineItem {
    val type = TimelineItemType.entries.firstOrNull { it.name == itemType } ?: TimelineItemType.UNKNOWN
    return TimelineItem(
        timelineItemId = timelineItemId,
        itemType = type,
        rawId = rawId,
        startAt = startAt?.parseLocalDateTime("startAt"),
        endAt = endAt?.parseLocalDateTime("endAt"),
        photoUrl =
            if (type == TimelineItemType.PHOTO) {
                ((payload as? JsonObject)?.get("photoUrl") as? JsonPrimitive)?.contentOrNull
            } else {
                null
            },
    )
}
