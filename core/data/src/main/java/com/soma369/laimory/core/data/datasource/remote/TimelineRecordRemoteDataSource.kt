package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.timeline.response.TimelineEventResponse
import kotlinx.serialization.json.JsonObject

/** 확정 타임라인 기록 API의 원격 호출 경로. */
interface TimelineRecordRemoteDataSource {
    suspend fun updateTimelineEvent(
        timelineEventId: Long,
        request: JsonObject,
    ): TimelineEventResponse
}
