package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.timeline.response.TimelineEventResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.Path

/** 서버 확정 타임라인 기록 API(`/a/api/{applicationVersion}/timeline`). 인증 필요. */
interface TimelineRecordApi {
    @PATCH("timeline/events/{timelineEventId}")
    suspend fun updateTimelineEvent(
        @Path("timelineEventId") timelineEventId: Long,
        @Body request: JsonObject,
    ): Response<ApiResponse<TimelineEventResponse>>

    @DELETE("timeline/events/{timelineEventId}")
    suspend fun deleteTimelineEvent(
        @Path("timelineEventId") timelineEventId: Long,
    ): Response<ApiResponse<Unit>>

    @DELETE("timeline/daily-records/{dailyRecordId}")
    suspend fun deleteDailyRecord(
        @Path("dailyRecordId") dailyRecordId: Long,
    ): Response<ApiResponse<Unit>>
}
