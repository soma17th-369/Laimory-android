package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.TimelineDraftRemoteDataSource
import com.soma369.laimory.core.data.model.timeline.request.CreateDraftTaskRequest
import com.soma369.laimory.core.data.model.timeline.request.PhotoUploadCreateRequest
import com.soma369.laimory.core.data.model.timeline.request.PhotoUploadItem
import com.soma369.laimory.core.data.model.timeline.request.toSourceItemDto
import com.soma369.laimory.core.data.model.timeline.response.toDomain
import com.soma369.laimory.core.data.network.s3.PhotoMetaResolver
import com.soma369.laimory.core.data.network.s3.S3PhotoUploader
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class TimelineDraftRepositoryImpl
    @Inject
    constructor(
        private val photoMetaResolver: PhotoMetaResolver,
        private val remote: TimelineDraftRemoteDataSource,
        private val s3Uploader: S3PhotoUploader,
        private val json: Json,
    ) : TimelineDraftRepository {
        override suspend fun uploadPhotos(clientPhotoUris: List<String>): List<String> {
            if (clientPhotoUris.isEmpty()) return emptyList()

            // presign 요청과 실제 PUT 이 정확히 같은 contentType/size 를 쓰도록 한 번만 산출한다.
            val metas = clientPhotoUris.map { photoMetaResolver.resolve(it) }
            val response =
                remote.requestPhotoUploads(
                    PhotoUploadCreateRequest(
                        photos = metas.map { PhotoUploadItem(contentType = it.contentType, size = it.size) },
                    ),
                )
            val uploads = response.uploads
            if (uploads.size != clientPhotoUris.size) {
                throw ApiException.UnknownException("발급된 업로드 URL 수가 사진 수와 다릅니다")
            }

            // 인덱스로 사진 ↔ 발급결과를 맞춘다(발급 응답에 식별자가 없음).
            clientPhotoUris.forEachIndexed { index, uri ->
                s3Uploader.upload(
                    clientPhotoUri = uri,
                    uploadUrl = uploads[index].uploadUrl,
                    contentType = metas[index].contentType,
                    size = metas[index].size,
                )
            }
            return uploads.map { it.filename }
        }

        override suspend fun createDraft(
            recordDate: LocalDate,
            zone: ZoneId,
            items: List<SourceItem>,
            uploadedPhotoFilenames: Map<String, String>,
        ): DraftTaskHandle {
            val request =
                CreateDraftTaskRequest(
                    recordAt = recordDate.atStartOfDay().toString(),
                    recordTimeZone = zone.id,
                    sourceItems = items.map { it.toSourceItemDto(json, uploadedPhotoFilenames[it.rawId]) },
                )
            return remote.createDraft(request).toDomain()
        }

        override suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot = remote.getDraftStatus(taskId).toDomain()
    }
