package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.timeline.request.CreateDraftTaskRequest
import com.soma369.laimory.core.data.model.timeline.request.PhotoUploadCreateRequest
import com.soma369.laimory.core.data.model.timeline.response.CreateDraftTaskResponse
import com.soma369.laimory.core.data.model.timeline.response.DraftTaskStatusResponse
import com.soma369.laimory.core.data.model.timeline.response.PhotoUploadCreateResponse

/** 타임라인 초안 API 3종의 원격 호출 경로. */
interface TimelineDraftRemoteDataSource {
    suspend fun requestPhotoUploads(request: PhotoUploadCreateRequest): PhotoUploadCreateResponse

    suspend fun createDraft(request: CreateDraftTaskRequest): CreateDraftTaskResponse

    suspend fun getDraftStatus(taskId: String): DraftTaskStatusResponse
}
