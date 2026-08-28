package com.soma369.laimory.core.data.model.timeline.request

import com.soma369.laimory.core.domain.model.timeline.CreateTimelineEventCommand
import kotlinx.serialization.Serializable

/**
 * Event 생성 요청.
 *
 * 서버는 `eventType`·`title`·`subtitle`·`startAt`·`endAt` 다섯 키를 모두 요구하고 누락은 400 이다.
 * 그중 `subtitle`·`endAt` 은 값이 nullable 이라 **기본값을 주지 않는다** — 요청 Json 이
 * `explicitNulls = true` 라 기본값 없는 nullable 은 `null` 로 그대로 실린다.
 *
 * 반대로 `memo`·`photosToAdd` 는 optional 이라 기본값을 줘 빈 값이면 키째 빠지게 한다.
 */
@Serializable
data class CreateTimelineEventRequest(
    val eventType: String,
    val title: String,
    val subtitle: String?,
    val startAt: String,
    val endAt: String?,
    val memo: String? = null,
    val photosToAdd: List<TimelineEventPhotoRequest>? = null,
)

internal fun CreateTimelineEventCommand.toRequest() =
    CreateTimelineEventRequest(
        eventType = eventType.name,
        title = title,
        subtitle = subtitle,
        startAt = startAt.toApiDateTime(),
        endAt = endAt?.toApiDateTime(),
        // 누락·null·blank 가 모두 메모 없음이라 빈 값이면 키를 생략한다.
        memo = memo?.takeIf(String::isNotBlank),
        photosToAdd = photosToAdd.takeIf { it.isNotEmpty() }?.map { it.toRequest() },
    )
