package com.soma369.laimory.core.data.model.timeline.request

import com.soma369.laimory.core.domain.model.timeline.TimelineEventUpdateField
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Event 수정(PATCH) 요청. sparse 갱신이라 **키 존재 자체가 의미**다.
 *
 * `title`·`startAt` 은 항상 보낸다. `subtitle`·`endAt` 은 키가 필수고 값이 nullable 이라 기본값을
 * 주지 않는다 — 요청 Json 이 `explicitNulls = true` 라 `null` 이 그대로 실린다.
 *
 * `eventType`·`photosToAdd` 는 안 보내면 안 바뀌므로 기본값을 준다. `memo` 만 지움(`null` 전송)과
 * 안 바꿈(키 생략)을 함께 표현해야 해서 [PatchField] 를 쓴다.
 */
@Serializable
data class UpdateTimelineEventRequest(
    val title: String,
    val subtitle: String?,
    val startAt: String,
    val endAt: String?,
    val eventType: String? = null,
    val memo: PatchField? = null,
    val photosToAdd: List<TimelineEventPhotoRequest>? = null,
)

internal fun UpdateTimelineEventCommand.toRequest() =
    UpdateTimelineEventRequest(
        title = title,
        subtitle = subtitle,
        startAt = startAt.toApiDateTime(),
        endAt = endAt?.toApiDateTime(),
        eventType = eventType?.name,
        memo = memo.orNull(::PatchField),
        photosToAdd = photosToAdd.orNull { photos -> photos.map { it.toRequest() } },
    )

/** `Unchanged` 는 키를 생략하도록 null 로, `Value` 는 [transform] 결과로 옮긴다. */
private fun <T, R> TimelineEventUpdateField<T>.orNull(transform: (T) -> R): R? =
    when (this) {
        TimelineEventUpdateField.Unchanged -> null
        is TimelineEventUpdateField.Value -> transform(value)
    }

internal fun LocalDateTime.toApiDateTime(): String = API_DATE_TIME_FORMATTER.format(this)

private val API_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
