package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import java.time.LocalDate

/** 서버에 저장된 확정 타임라인 기록의 조회·편집 계약. */
interface TimelineRecordRepository {
    /** 저장된 DailyRecord 전체 목록을 서버 정렬 그대로 조회한다. 결과가 없으면 빈 목록을 반환한다. */
    suspend fun getDailyRecords(): List<DailyTimeline>

    /** DailyRecord 한 건을 하위 Event·Item graph와 함께 조회한다. */
    suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline

    /** 통합 PATCH로 Event 상세·타입·메모를 수정하고 업로드 완료 PHOTO를 append한다. */
    suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent

    /** Event와 하위 Item을 삭제한다. 마지막 Event여도 DailyRecord는 유지한다. */
    suspend fun deleteEvent(timelineEventId: Long)

    /** DailyRecord와 하위 Event·Item 전체를 삭제한다. */
    suspend fun deleteDailyRecord(recordDate: LocalDate)
}
