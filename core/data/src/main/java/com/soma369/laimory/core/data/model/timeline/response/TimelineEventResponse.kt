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
    /** 장소 자리에 `{place}` 가 들어 있을 수 있는 원문. 채우는 일은 도메인이 한다. */
    val subtitle: String? = null,
    val memo: String? = null,
    /** AI 가 Event 마다 생성해 되묻는 문장. 서버 편집 API 로 바꿀 수 없는 읽기 전용 값이다. */
    val question: String? = null,
    // 아래 장소 필드는 모두 선택이다 — 장소 후보 미배포 서버 응답도 그대로 읽혀야 한다.
    val place: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** 배열 순서가 우선순위다. 서버는 없으면 `[]` 를 주지만 미배포 서버는 키가 없다. */
    val placeCandidates: List<PlaceCandidateResponse> = emptyList(),
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
        place = place,
        address = address,
        // 서버는 둘을 항상 함께 주거나 함께 비운다. 한쪽만 오면 좌표로 쓸 수 없으니 둘 다 버린다.
        latitude = latitude?.takeIf { longitude != null },
        longitude = longitude?.takeIf { latitude != null },
        placeCandidates = placeCandidates.map(PlaceCandidateResponse::name).filter(String::isNotBlank),
        items = items.map(TimelineItemResponse::toDomain),
    )

internal fun String.parseLocalDateTime(fieldName: String): LocalDateTime =
    runCatching { LocalDateTime.parse(this) }
        .getOrElse { throw ApiException.UnknownException("잘못된 $fieldName 형식입니다: $this") }
