package com.soma369.laimory.core.domain.model.analytics

/**
 * 완료 순간의 이벤트 한 건. 요약에 필요한 값만 받아 화면 모델에 기대지 않는다.
 *
 * 원문은 여기서 끝난다 — 요약은 질문·메모가 "있는지" 만 세고 내용은 보내지 않는다.
 */
data class AnalyticsTimelineEventSnapshot(
    val timelineEventId: Long,
    val question: String?,
    val memo: String?,
)
