package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.timeline.request.SaveDailyRecordRequest
import com.soma369.laimory.core.data.model.timeline.request.UpdateTimelineEventMemoRequest
import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineListResponse
import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineResponse
import com.soma369.laimory.core.data.model.timeline.response.MonthlyDailyRecordListResponse
import com.soma369.laimory.core.data.model.timeline.response.TimelineEventResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** 서버 확정 타임라인 기록 API(`/a/api/{applicationVersion}/timeline`). 인증 필요. */
interface TimelineRecordApi {
    @GET("timeline/daily-records")
    suspend fun getDailyRecords(): Response<ApiResponse<DailyTimelineListResponse>>

    /** 캘린더 탐색용 월별 조회. `year`·`month` 는 필수이며 서버가 `1000..9999` 범위를 벗어나면 거절한다. */
    @GET("timeline/monthly-records")
    suspend fun getMonthlyDailyRecords(
        @Query("year") year: Int,
        @Query("month") month: Int,
    ): Response<ApiResponse<MonthlyDailyRecordListResponse>>

    @GET("timeline/daily-records/{recordDate}")
    suspend fun getDailyRecord(
        @Path("recordDate") recordDate: String,
    ): Response<ApiResponse<DailyTimelineResponse>>

    @GET("timeline/events/{timelineEventId}")
    suspend fun getTimelineEvent(
        @Path("timelineEventId") timelineEventId: Long,
    ): Response<ApiResponse<TimelineEventResponse>>

    @PATCH("timeline/events/{timelineEventId}")
    suspend fun updateTimelineEvent(
        @Path("timelineEventId") timelineEventId: Long,
        @Body request: JsonObject,
    ): Response<ApiResponse<Unit>>

    @PUT("timeline/events/{timelineEventId}/memo")
    suspend fun updateTimelineEventMemo(
        @Path("timelineEventId") timelineEventId: Long,
        @Body request: UpdateTimelineEventMemoRequest,
    ): Response<ApiResponse<Unit>>

    @DELETE("timeline/events/{timelineEventId}")
    suspend fun deleteTimelineEvent(
        @Path("timelineEventId") timelineEventId: Long,
    ): Response<ApiResponse<Unit>>

    @DELETE("timeline/events/{timelineEventId}/items/{timelineItemId}")
    suspend fun deleteTimelineEventPhoto(
        @Path("timelineEventId") timelineEventId: Long,
        @Path("timelineItemId") timelineItemId: Long,
    ): Response<ApiResponse<Unit>>

    @DELETE("timeline/daily-records/{recordDate}")
    suspend fun deleteDailyRecord(
        @Path("recordDate") recordDate: String,
    ): Response<ApiResponse<Unit>>

    @POST("timeline/daily-records/{recordDate}/save")
    suspend fun saveDailyRecord(
        @Path("recordDate") recordDate: String,
        @Body request: SaveDailyRecordRequest,
    ): Response<ApiResponse<Unit>>
}
