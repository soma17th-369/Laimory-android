package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * `POST /timeline/drafts` 요청. 하루치 수집 아이템으로 초안 생성 작업을 만든다.
 *
 * [recordDate]/[recordAt]/[timelineWindow] 는 상호 파생 관계가 없는 독립 값이며 서버는 정합성을
 * 검증하지 않는다 — 다음날 아침에 쓰는 어제 일기처럼 [recordAt] 과 [recordDate] 의 날짜가 달라도 된다.
 *
 * - [recordDate]: 기록이 속하는 날(선택 날짜, `yyyy-MM-dd`). 클라이언트가 단일 권위다 —
 *   서버는 계산·보정 없이 그대로 쓴다(과거 서버 정오 경계 파생은 삭제됨).
 * - [recordAt]: 사용자가 실제로 기록을 만든 시각. 오프셋 없는 로컬 datetime 문자열이고,
 *   기준 시간대는 [recordTimeZone] 로 따로 보낸다. 서버는 아무것도 파생하지 않는 메타데이터다.
 * - [timelineWindow]: AI 가 이번 요청에서 이벤트를 만들 시간 범위. 서버는 필수값과
 *   `startTime < endTime` 만 검증해 값 변형 없이 AI 에 전달한다.
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
 * AI 이벤트 생성 범위. `RecordDateWindow` 의 `Instant start/end` 를 요청 zone 의
 * 오프셋 없는 로컬 datetime 문자열로 렌더해 담는다.
 */
@Serializable
data class TimelineWindowDto(
    val startTime: String,
    val endTime: String,
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
