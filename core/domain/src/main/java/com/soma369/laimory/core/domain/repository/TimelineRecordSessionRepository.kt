package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * 프로세스가 살아 있는 동안 현재 타임라인 결과를 공유하는 상태 홀더 계약.
 *
 * 서버 재조회 API가 아직 없으므로 draft 폴링 SUCCESS 결과를 저장하고, 이후 편집·삭제 결과를 같은
 * 상태에 원자적으로 반영한다. 프로세스 종료 시 상태는 의도적으로 소멸한다.
 */
interface TimelineRecordSessionRepository {
    val timeline: StateFlow<DailyTimeline?>

    fun save(timeline: DailyTimeline)

    fun replaceEvent(event: TimelineEvent)

    fun removeEvent(timelineEventId: Long)

    fun clear()
}
