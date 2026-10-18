package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.timeline.request.CreateDraftTaskRequest
import com.soma369.laimory.core.data.model.timeline.request.DraftCreatableCheckRequest
import com.soma369.laimory.core.data.model.timeline.request.PhotoUploadCreateRequest
import com.soma369.laimory.core.data.model.timeline.response.CreateDraftTaskResponse
import com.soma369.laimory.core.data.model.timeline.response.DraftCreatableCheckResponse
import com.soma369.laimory.core.data.model.timeline.response.DraftTaskStatusResponse
import com.soma369.laimory.core.data.model.timeline.response.PhotoUploadCreateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** 서버 타임라인 초안 API(`/a/api/{applicationVersion}/timeline/drafts`). 인증 필요. */
interface TimelineDraftApi {
    /** presigned 업로드 URL 발급. */
    @POST("timeline/drafts/photo-uploads")
    suspend fun requestPhotoUploads(
        @Body request: PhotoUploadCreateRequest,
    ): Response<ApiResponse<PhotoUploadCreateResponse>>

    /** 초안 생성 작업 요청(비동기). */
    @POST("timeline/drafts")
    suspend fun createDraft(
        @Body request: CreateDraftTaskRequest,
    ): Response<ApiResponse<CreateDraftTaskResponse>>

    /** 이 목록으로 타임라인을 만들 수 있는지 확인한다. 부수효과가 없어 반복 호출해도 된다. */
    @POST("timeline/drafts/check")
    suspend fun checkCreatable(
        @Body request: DraftCreatableCheckRequest,
    ): Response<ApiResponse<DraftCreatableCheckResponse>>

    /** 초안 생성 작업 상태 조회. */
    @GET("timeline/drafts/{taskId}")
    suspend fun getDraftStatus(
        @Path("taskId") taskId: String,
    ): Response<ApiResponse<DraftTaskStatusResponse>>
}
