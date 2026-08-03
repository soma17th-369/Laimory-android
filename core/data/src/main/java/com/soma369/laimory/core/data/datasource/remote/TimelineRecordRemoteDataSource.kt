package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineListResponse
import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineResponse
import com.soma369.laimory.core.data.model.timeline.response.TimelineEventResponse
import kotlinx.serialization.json.JsonObject

/** 확정 타임라인 기록 API의 원격 호출 경로. */
interface TimelineRecordRemoteDataSource {
    /** 확정된 전체 일별 타임라인을 서버 정렬 순서대로 조회한다. */
    suspend fun getDailyRecords(): DailyTimelineListResponse

    /** 식별자에 해당하는 일별 타임라인 상세를 조회한다. */
    suspend fun getDailyRecord(dailyRecordId: Long): DailyTimelineResponse

    /** 수정 이후 서버에 저장된 최신 Event를 조회한다. */
    suspend fun getTimelineEvent(timelineEventId: Long): TimelineEventResponse

    /**
     * Event를 수정한다.
     *
     * 성공 응답의 body는 null이며, 수정된 Event가 필요하면 별도로 조회해야 한다.
     */
    suspend fun updateTimelineEvent(
        timelineEventId: Long,
        request: JsonObject,
    )

    /** Event와 연결된 데이터를 삭제한다. */
    suspend fun deleteTimelineEvent(timelineEventId: Long)

    /** 하루 기록과 하위 Event를 삭제한다. */
    suspend fun deleteDailyRecord(dailyRecordId: Long)
}
