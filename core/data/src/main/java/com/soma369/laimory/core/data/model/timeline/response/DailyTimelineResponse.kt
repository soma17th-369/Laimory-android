package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.timeline.DailyRecordStatus
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import kotlinx.serialization.Serializable
import java.time.LocalDate

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
        emotion =
            emotionType?.let { raw ->
                TimelineEmotion.entries.firstOrNull { it.name == raw } ?: TimelineEmotion.UNKNOWN
            },
        events = events.map(TimelineEventResponse::toDomain),
        // 미지원 문자열은 저장 가능 상태로 추정하지 않도록 null(읽기 전용)로 수렴한다.
        status = status?.let { raw -> DailyRecordStatus.entries.firstOrNull { it.name == raw } },
    )

private fun String.parseLocalDate(fieldName: String): LocalDate =
    runCatching { LocalDate.parse(this) }
        .getOrElse { throw ApiException.UnknownException("잘못된 $fieldName 형식입니다: $this") }
