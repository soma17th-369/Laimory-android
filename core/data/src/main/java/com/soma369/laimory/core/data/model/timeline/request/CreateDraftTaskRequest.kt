package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * `POST /timeline/drafts` 요청. 선택한 기록 창의 수집 아이템으로 초안 생성 작업을 만든다.
 *
 * [recordDate]는 초안의 기준 날짜, [timelineWindow]는 실제 포함 구간이다. [recordAt]은 요청 시점의
 * 오프셋 없는 로컬 datetime 문자열이고, 기준 시간대는 [recordTimeZone]으로 따로 보낸다.
 */
@Serializable
data class CreateDraftTaskRequest(
    val recordDate: String,
    val recordAt: String,
    val recordTimeZone: String,
    val timelineWindow: TimelineWindowDto,
    val sourceItems: List<SourceItemDto>,
)

/**
 * 초안 요청의 원천 아이템 1건.
 *
 * [payload] 는 [itemType] 별로 모양이 다른 판별 유니온이라 [JsonObject] 로 담는다
 * (projection 이 타입별 payload DTO 를 직렬화해 채운다).
 */
@Serializable
data class SourceItemDto(
    val itemType: String,
    val rawId: String,
    val startAt: String,
    val endAt: String?,
    val payload: JsonObject,
)
