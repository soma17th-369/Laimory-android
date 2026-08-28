package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 하루 기록에 새 Event 를 만드는 명령.
 *
 * 수정([UpdateTimelineEventCommand])과 달리 **키를 생략하는 개념이 없다.** 서버가
 * `eventType`·`title`·`subtitle`·`startAt`·`endAt` 다섯 키를 모두 요구하므로(누락은 400) 값이
 * 없는 것과 키가 없는 것을 구분할 필요가 없고, 그래서 nullable 값으로 그대로 표현한다.
 *
 * 시각은 **보낸 값 그대로 저장된다** — 수정과 달리 겹침 보정이 없어 기존 이벤트와 같은 시각도
 * 그대로 들어간다. 호출부가 기본값을 정할 때 이 점을 안고 정해야 한다.
 */
data class CreateTimelineEventCommand(
    val recordDate: LocalDate,
    val eventType: TimelineEventType,
    val title: String,
    val subtitle: String?,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    /** 누락·null·blank 는 메모 없음이다. */
    val memo: String?,
    /** 빈 목록은 사진 없음이다. */
    val photosToAdd: List<TimelineEventPhotoAddition> = emptyList(),
)
