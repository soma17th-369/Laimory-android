package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.timeline.request.CreateDraftTaskRequest
import com.soma369.laimory.core.data.model.timeline.request.PhotoUploadCreateRequest
import com.soma369.laimory.core.data.model.timeline.response.CreateDraftTaskResponse
import com.soma369.laimory.core.data.model.timeline.response.DraftTaskStatusResponse
import com.soma369.laimory.core.data.model.timeline.response.PhotoUploadCreateResponse
import com.soma369.laimory.core.data.network.api.TimelineDraftApi
import com.soma369.laimory.core.data.network.safeApiCall
import javax.inject.Inject

class TimelineDraftRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: TimelineDraftApi,
    ) : TimelineDraftRemoteDataSource {
        override suspend fun requestPhotoUploads(request: PhotoUploadCreateRequest): PhotoUploadCreateResponse =
            safeApiCall { api.requestPhotoUploads(request) }

        override suspend fun createDraft(request: CreateDraftTaskRequest): CreateDraftTaskResponse =
            safeApiCall { api.createDraft(request) }

        override suspend fun getDraftStatus(taskId: String): DraftTaskStatusResponse = safeApiCall { api.getDraftStatus(taskId) }
    }
