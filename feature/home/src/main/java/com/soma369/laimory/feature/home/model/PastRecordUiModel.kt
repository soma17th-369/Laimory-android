package com.soma369.laimory.feature.home.model

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineItem
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import com.soma369.laimory.core.ui.theme.Emotion
import java.time.LocalDate
import java.time.LocalDateTime

/** 홈 지난 기록 카드 한 장의 표시 데이터. */
@Immutable
data class PastRecordUiModel(
    val dailyRecordId: Long,
    val recordDate: LocalDate,
    /** 서버 감정을 표시 팔레트로 옮긴 값. 감정이 없거나 미지 literal이면 null(중립 표시). */
    val emotion: Emotion?,
    /** 가장 이른 Event의 title·subtitle로 구성한 대표 문구. Event가 없으면 null. */
    val summary: String?,
    /** 가장 이른 PHOTO Item의 대표 이미지 URL. PHOTO가 없으면 null. */
    val photoUrl: String?,
)

internal fun DailyTimeline.toPastRecordUiModel(): PastRecordUiModel =
    PastRecordUiModel(
        dailyRecordId = dailyRecordId,
        recordDate = recordDate,
        emotion = emotion?.toUiEmotionOrNull(),
        summary =
            events.firstOrNull()?.let { event ->
                event.subtitle?.let { subtitle -> "${event.title} · $subtitle" } ?: event.title
            },
        photoUrl =
            events
                .flatMap(TimelineEvent::items)
                .filter { it.itemType == TimelineItemType.PHOTO && it.photoUrl != null }
                // 서버 Item 계약과 동일하게 startAt null-first, 동률은 timelineItemId 오름차순.
                .minWithOrNull(
                    compareBy<TimelineItem, LocalDateTime?>(nullsFirst(), TimelineItem::startAt)
                        .thenBy(TimelineItem::timelineItemId),
                )?.photoUrl,
    )

private fun TimelineEmotion.toUiEmotionOrNull(): Emotion? =
    when (this) {
        TimelineEmotion.VERY_HAPPY -> Emotion.JOY
        TimelineEmotion.HAPPY -> Emotion.CALM
        TimelineEmotion.NEUTRAL -> Emotion.MELLOW
        TimelineEmotion.UNHAPPY -> Emotion.WEARY
        TimelineEmotion.VERY_UNHAPPY -> Emotion.DOWN
        TimelineEmotion.UNKNOWN -> null
    }
