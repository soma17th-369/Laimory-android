package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDateTime

/** AI가 구성한 타임라인의 Event와 소속 Item 목록. */
data class TimelineEvent(
    val timelineEventId: Long,
    val eventType: TimelineEventType,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    val title: String,
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
)
