package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.timeline.request.CreateTimelineEventRequest
import com.soma369.laimory.core.data.model.timeline.request.SaveDailyRecordRequest
import com.soma369.laimory.core.data.model.timeline.request.UpdateDailyRecordEmotionRequest
import com.soma369.laimory.core.data.model.timeline.request.UpdateTimelineEventMemoRequest
import com.soma369.laimory.core.data.model.timeline.request.UpdateTimelineEventRequest
import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineListResponse
import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineResponse
import com.soma369.laimory.core.data.model.timeline.response.MonthlyDailyRecordListResponse
import com.soma369.laimory.core.data.model.timeline.response.TimelineEventResponse
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
        @Body request: UpdateTimelineEventRequest,
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

    /**
     * 하루 기록에 새 Event 를 만든다. DRAFT·SAVED 모두 허용하며 DailyRecord 를 자동 생성하지 않는다.
     *
     */
    @POST("timeline/daily-records/{recordDate}/events")
    suspend fun createTimelineEvent(
        @Path("recordDate") recordDate: String,
        @Body request: CreateTimelineEventRequest,
    ): Response<ApiResponse<TimelineEventResponse>>

    /**
     * 저장된 하루 기록의 감정만 교체한다. 응답 envelope 의 `body` 는 null 이다(HTTP 무바디가 아니다).
     *
     * 대상은 **SAVED 기록뿐**이다 — DRAFT 의 최초 감정 확정은 [saveDailyRecord] 가 담당하고,
     * DRAFT 에 요청하면 `409/-1020` 이다.
     */
    @PUT("timeline/daily-records/{recordDate}/emotion")
    suspend fun updateDailyRecordEmotion(
        @Path("recordDate") recordDate: String,
        @Body request: UpdateDailyRecordEmotionRequest,
    ): Response<ApiResponse<Unit>>

    @POST("timeline/daily-records/{recordDate}/save")
    suspend fun saveDailyRecord(
        @Path("recordDate") recordDate: String,
        @Body request: SaveDailyRecordRequest,
    ): Response<ApiResponse<Unit>>
}
