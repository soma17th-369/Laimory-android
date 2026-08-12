package com.soma369.laimory.feature.timeline.model

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.timeline.DailyRecordStatus
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import java.time.LocalDate
import java.time.LocalDateTime

/** [isEditable]은 서버 status가 SAVED가 아닐 때 true — 저장 CTA·편집·삭제 진입의 정본이다. */
@Immutable
data class TimelineRecordUiModel(
    val dailyRecordId: Long,
    val recordDate: LocalDate,
    val events: List<TimelineEventUiModel>,
    val isEditable: Boolean,
)

@Immutable
data class TimelineEventUiModel(
    val timelineEventId: Long,
    val eventType: TimelineEventType,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    val title: String,
    val subtitle: String?,
    val memo: String?,
    val itemCounts: List<TimelineItemCountUiModel>,
    val photoUrls: List<String?> = emptyList(),
)

@Immutable
data class TimelineItemCountUiModel(
    val itemType: TimelineItemType,
    val count: Int,
)

internal fun DailyTimeline.toUiModel() =
    TimelineRecordUiModel(
        dailyRecordId = dailyRecordId,
        recordDate = recordDate,
        events = events.map(TimelineEvent::toUiModel),
        // 상태 미상(null·미지원 값)은 작성 중으로 간주한다 — status 미배포 서버와의 결합을 없앤다.
        isEditable = status != DailyRecordStatus.SAVED,
    )

private fun TimelineEvent.toUiModel() =
    TimelineEventUiModel(
        timelineEventId = timelineEventId,
        eventType = eventType,
        startAt = startAt,
        endAt = endAt,
        title = title,
        subtitle = subtitle,
        memo = memo,
        itemCounts =
            TimelineItemType.entries.mapNotNull { type ->
                items.count { it.itemType == type }
                    .takeIf { it > 0 }
                    ?.let { count -> TimelineItemCountUiModel(type, count) }
            },
        photoUrls =
            items
                .filter { it.itemType == TimelineItemType.PHOTO }
                .map { item -> item.photoUrl },
    )
