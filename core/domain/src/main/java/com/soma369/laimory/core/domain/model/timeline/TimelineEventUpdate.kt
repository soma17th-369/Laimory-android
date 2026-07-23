package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDateTime

/**
 * Event PATCH의 optional 키 상태.
 *
 * [Unchanged]는 JSON 키를 보내지 않고, [Value]는 값 자체를 전송한다. nullable 값을 담은 [Value]는
 * 명시적 null을 의미하므로 memo 제거처럼 서버가 키 존재 여부를 구분하는 필드에 사용한다.
 */
sealed interface TimelineEventUpdateField<out T> {
    data object Unchanged : TimelineEventUpdateField<Nothing>

    data class Value<T>(
        val value: T,
    ) : TimelineEventUpdateField<T>
}

/** Event PATCH로 append할 업로드 완료 PHOTO 정보. */
data class TimelineEventPhotoAddition(
    val rawId: String,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val filename: String,
    val clientPhotoUri: String,
    val latitude: Double?,
    val longitude: Double?,
)

/**
 * 서버의 통합 Event PATCH 명령.
 *
 * title·subtitle·startAt·endAt은 항상 전송하고, 나머지는 서버 계약에 맞춰 키 존재 여부를 구분한다.
 */
data class UpdateTimelineEventCommand(
    val timelineEventId: Long,
    val title: String,
    val subtitle: String?,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    val eventType: TimelineEventType? = null,
    val memo: TimelineEventUpdateField<String?> = TimelineEventUpdateField.Unchanged,
    val photosToAdd: TimelineEventUpdateField<List<TimelineEventPhotoAddition>> = TimelineEventUpdateField.Unchanged,
)
