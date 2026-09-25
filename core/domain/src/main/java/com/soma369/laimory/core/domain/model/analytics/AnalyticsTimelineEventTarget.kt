package com.soma369.laimory.core.domain.model.analytics

import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import java.time.LocalDate

/**
 * 편집 이벤트(메모 저장·수정·삭제)가 공통으로 싣는 대상 사건.
 *
 * 세션의 하루 기록에서 뽑는다 — 화면 모델에 기대지 않아 두 편집 화면이 같은 값을 만든다. 원문(제목·메모)은
 * 여기서 끝나고, 종류·사진 수·기록 상태만 남는다.
 *
 * @property recordState 행동한 시점의 기록 상태.
 */
data class AnalyticsTimelineEventTarget(
    val timelineEventId: Long,
    val eventType: TimelineEventType,
    val photoCount: Int,
    val recordState: AnalyticsTimelineState,
    val recordDate: LocalDate,
) {
    companion object {
        /** 그 사건이 기록에 없으면 null. */
        fun of(
            timeline: DailyTimeline,
            timelineEventId: Long,
        ): AnalyticsTimelineEventTarget? {
            val event = timeline.events.firstOrNull { it.timelineEventId == timelineEventId } ?: return null
            return AnalyticsTimelineEventTarget(
                timelineEventId = event.timelineEventId,
                eventType = event.eventType,
                photoCount = event.items.count { it.itemType == TimelineItemType.PHOTO },
                recordState = AnalyticsTimelineState.of(timeline.status),
                recordDate = timeline.recordDate,
            )
        }
    }
}
