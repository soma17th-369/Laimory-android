package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.model.timeline.DailyRecordStatus
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import kotlinx.serialization.Serializable

@Serializable
data class DailyTimelineResponse(
    val dailyRecordId: Long,
    val recordDate: String,
    val emotionType: String? = null,
    val events: List<TimelineEventResponse>,
    val status: String? = null,
)

internal fun DailyTimelineResponse.toDomain(): DailyTimeline =
    DailyTimeline(
        dailyRecordId = dailyRecordId,
        recordDate = recordDate.parseLocalDate("recordDate"),
        emotion = emotionType.toTimelineEmotionOrNull(),
        events = events.map(TimelineEventResponse::toDomain),
        // 미지원 문자열은 상태 미상(null)으로 수렴한다 — SAVED 판별은 화면 정책이 담당한다.
        status = status?.let { raw -> DailyRecordStatus.entries.firstOrNull { it.name == raw } },
    )
