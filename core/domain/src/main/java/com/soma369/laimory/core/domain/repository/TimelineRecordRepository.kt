package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.timeline.CreateTimelineEventCommand
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import java.time.LocalDate
import java.time.YearMonth

/** 서버에 저장된 확정 타임라인 기록의 조회·편집 계약. */
interface TimelineRecordRepository {
    /** 저장된 DailyRecord 전체 목록을 서버 정렬 그대로 조회한다. 결과가 없으면 빈 목록을 반환한다. */
    suspend fun getDailyRecords(): List<DailyTimeline>

    /** DailyRecord 한 건을 하위 Event·Item graph와 함께 조회한다. */
    suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline

    /** 하루 기록에 새 Event 를 만든다. DRAFT·SAVED 모두 허용한다. */
    suspend fun createEvent(command: CreateTimelineEventCommand): TimelineEvent

    /** 통합 PATCH로 Event 상세·타입·메모를 수정하고 업로드 완료 PHOTO를 append한다. */
    suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent

    /** 전용 PUT으로 Event 메모를 작성·수정·제거한다. */
    suspend fun updateEventMemo(
        timelineEventId: Long,
        memo: String?,
    ): TimelineEvent

    /** Event와 하위 Item을 삭제한다. 마지막 Event여도 DailyRecord는 유지한다. */
    suspend fun deleteEvent(timelineEventId: Long)

    /** Event와 PHOTO Item의 연결을 해제한다. 사진 원본의 즉시 삭제를 의미하지 않는다. */
    suspend fun deleteEventPhoto(
        timelineEventId: Long,
        timelineItemId: Long,
    )

    /** DailyRecord와 하위 Event·Item 전체를 삭제한다. */
    suspend fun deleteDailyRecord(recordDate: LocalDate)

    /** 표시 월의 기록 날짜와 감정만 조회한다. 캘린더 탐색용이라 Event graph 는 포함하지 않는다. */
    suspend fun getMonthlyDailyRecords(month: YearMonth): List<MonthlyDailyRecord>

    /**
     * 저장된 하루 기록의 감정만 교체한다. status 는 바뀌지 않고 같은 값 재요청도 멱등 성공이다.
     *
     * DRAFT 의 최초 감정 확정은 [saveDailyRecord] 가 담당한다 — DRAFT 에 부르면 `409/-1020` 이다.
     */
    suspend fun updateDailyRecordEmotion(
        recordDate: LocalDate,
        emotion: TimelineEmotion,
    )

    /** 전용 POST로 선택한 하루 감정과 함께 DRAFT 하루 기록을 SAVED로 확정한다. 성공 응답의 body는 null이다. */
    suspend fun saveDailyRecord(
        recordDate: LocalDate,
        emotion: TimelineEmotion,
    )
}
