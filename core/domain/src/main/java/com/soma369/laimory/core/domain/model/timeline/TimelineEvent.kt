package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDateTime

/** AI가 구성한 타임라인의 Event와 소속 Item 목록. */
data class TimelineEvent(
    val timelineEventId: Long,
    val eventType: TimelineEventType,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    val title: String,
    /**
     * 서버가 저장한 원문. 장소 자리에 [PLACE_PLACEHOLDER] 가 들어 있을 수 있어 그대로 그리면 안 된다 —
     * 표시는 [displaySubtitle] 로 한다. 편집 저장이 템플릿을 지키려면 원문이 필요해 그대로 둔다.
     */
    val subtitle: String?,
    val memo: String?,
    /**
     * AI 가 이 Event 에 대해 되묻는 문장. 값이 없으면 null 이다.
     *
     * 메모가 비어 있을 때 안내 문구를 대신하는 prompt 로 쓴다. 답을 유도하는 값이지 보존 대상이 아니라
     * 사용자가 메모를 남기면 그 메모가 질문을 대체한다.
     */
    val question: String?,
    val items: List<TimelineItem>,
    /**
     * 지금 장소. 후보가 있으면 서버가 처음에 1번 후보로 채우고, 장소 지정 API 로 바뀐다.
     *
     * "고르지 않음" 상태는 없다 — 1번 후보가 곧 처음 장소다. 그래서 이 값만으로는 사용자가 골랐는지
     * 알 수 없다.
     */
    val place: String? = null,
    /** 편집할 수 없는 주소. [place] 가 비었을 때 부제 장소 자리를 대신 채운다. */
    val address: String? = null,
    /**
     * 대표 좌표. 처음엔 AI 관측 위치(이동은 도착지)거나 1번 후보의 좌표이고, 좌표가 있는 후보를 고르면 그 좌표로
     * 바뀐다. 직접 입력으로 장소를 바꾸면 그대로 남는다. [longitude] 와 항상 함께 null 이거나 함께 값이다.
     */
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** AI 가 준 장소 후보. 순서가 우선순위다. 장소를 바꾼 뒤에도, 저장한 뒤에도 남는다. */
    val placeCandidates: List<PlaceCandidate> = emptyList(),
)
