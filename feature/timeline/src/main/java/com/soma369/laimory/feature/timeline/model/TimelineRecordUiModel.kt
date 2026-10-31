package com.soma369.laimory.feature.timeline.model

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.timeline.DailyRecordStatus
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.PlaceCandidate
import com.soma369.laimory.core.domain.model.timeline.PlaceSubtitle
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import com.soma369.laimory.core.domain.model.timeline.displaySubtitle
import com.soma369.laimory.core.domain.model.timeline.placeSubtitle
import com.soma369.laimory.core.domain.model.timeline.placeSubtitleOf
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
    /**
     * 하루를 대표하는 감정. 아직 고르지 않았으면 null 이다.
     *
     * DRAFT 는 언제나 null 이다 — 최초 감정은 작성 완료가 정한다.
     */
    val emotion: TimelineEmotion? = null,
)

@Immutable
data class TimelineEventUiModel(
    val timelineEventId: Long,
    val eventType: TimelineEventType,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    val title: String,
    /** 장소 자리를 채운 표시 문장. 서버 원문(템플릿)은 담지 않는다. */
    val subtitle: String?,
    val memo: String?,
    /** AI 가 되묻는 문장. 메모가 비어 있을 때 안내 문구를 대신하는 prompt 로 쓴다. */
    val question: String?,
    val itemCounts: List<TimelineItemCountUiModel>,
    val photoUrls: List<String?> = emptyList(),
    /**
     * 부제를 장소 칩 앞·장소·뒤로 나눈 것. **장소를 다시 고를 수 있는 이벤트에만** 있다 — 후보가
     * 있고 부제에 장소 자리가 남아 있을 때다. 칩을 그릴지는 이 값과 화면 모드가 정한다.
     */
    val placeSentence: PlaceSubtitle? = null,
    /** AI 가 준 장소 후보. 순서가 우선순위다. 화면은 이름만 쓰고, 좌표는 고를 때 함께 보낸다. */
    val placeCandidates: List<PlaceCandidate> = emptyList(),
    /** 장소 자리가 남은 서버 부제 원문. [placeSentence] 가 있을 때만 들고, 새 장소로 다시 채울 때 쓴다. */
    val subtitleTemplate: String? = null,
    /** 이벤트 좌표. 이름이 같은 후보 중 지금 장소가 어느 것인지 가릴 때 쓴다. */
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    /** 지금 장소. 고를 수 있는 이벤트에만 의미가 있다. */
    val place: String?
        get() = placeSentence?.place

    /**
     * 장소를 [place] 로 바꾼 모습. 서버 응답을 기다리는 동안 바뀐 장소를 먼저 보여 줄 때 쓴다.
     *
     * 원문 템플릿에서 다시 채우므로 장소 자리가 여럿이어도 모두 함께 바뀐다.
     */
    fun withPlace(place: String): TimelineEventUiModel {
        val sentence = placeSubtitleOf(subtitleTemplate, place) ?: return this
        return copy(
            subtitle = sentence.text,
            placeSentence = sentence,
        )
    }
}

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
        emotion = emotion,
    )

private fun TimelineEvent.toUiModel() =
    TimelineEventUiModel(
        timelineEventId = timelineEventId,
        eventType = eventType,
        startAt = startAt,
        endAt = endAt,
        title = title,
        subtitle = displaySubtitle,
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
        placeSentence = placeSubtitle()?.takeIf { placeCandidates.isNotEmpty() },
        placeCandidates = placeCandidates,
        subtitleTemplate = subtitle.takeIf { placeSubtitle() != null && placeCandidates.isNotEmpty() },
        latitude = latitude,
        longitude = longitude,
    )
