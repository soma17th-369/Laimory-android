package com.soma369.laimory.feature.timeline.model

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.timeline.DailyRecordStatus
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import com.soma369.laimory.feature.timeline.state.TimelineRecordMode
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * [isSaved]는 서버가 관리하는 하루 기록 상태다.
 *
 * 화면의 읽기·편집 여부로 쓰지 않는다 — 그건 `TimelineRecordMode` 가 정한다. SAVED 기록도 편집 모드에
 * 들어갈 수 있고, 서버는 Event 수정·메모·삭제를 허용한다. 이 값이 가르는 것은 저장 CTA 노출과
 * 최초 진입 모드뿐이다.
 */
@Immutable
data class TimelineRecordUiModel(
    val dailyRecordId: Long,
    val recordDate: LocalDate,
    val events: List<TimelineEventUiModel>,
    val isSaved: Boolean,
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
    /** AI 가 되묻는 문장. 메모가 비어 있을 때 안내 문구를 대신하는 prompt 로 쓴다. */
    val question: String?,
    val itemCounts: List<TimelineItemCountUiModel>,
    val photoUrls: List<String?> = emptyList(),
)

@Immutable
data class TimelineItemCountUiModel(
    val itemType: TimelineItemType,
    val count: Int,
)

/**
 * 최초 진입 모드.
 *
 * DRAFT 는 아직 다듬는 중이라 편집으로 열고, SAVED 는 읽기로 연다. 이후 모드 전환은 사용자가 정한다.
 */
internal fun TimelineRecordUiModel.initialMode(): TimelineRecordMode = if (isSaved) TimelineRecordMode.READ else TimelineRecordMode.EDIT

internal fun DailyTimeline.toUiModel() =
    TimelineRecordUiModel(
        dailyRecordId = dailyRecordId,
        recordDate = recordDate,
        events = events.map(TimelineEvent::toUiModel),
        // 상태 미상(null·미지원 값)은 작성 중으로 간주한다 — status 미배포 서버와의 결합을 없앤다.
        isSaved = status == DailyRecordStatus.SAVED,
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
        question = question,
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
