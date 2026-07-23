package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand

/** 서버에 저장된 확정 타임라인 기록의 조회·편집 계약. */
interface TimelineRecordRepository {
    /** 통합 PATCH로 Event 상세·타입·메모를 수정하고 업로드 완료 PHOTO를 append한다. */
    suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent
}
