package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.TimelineDraftRemoteDataSource
import com.soma369.laimory.core.data.model.timeline.request.CreateDraftTaskRequest
import com.soma369.laimory.core.data.model.timeline.request.PhotoUploadCreateRequest
import com.soma369.laimory.core.data.model.timeline.request.PhotoUploadItem
import com.soma369.laimory.core.data.model.timeline.request.TimelineWindowDto
import com.soma369.laimory.core.data.model.timeline.request.toSourceItemDto
import com.soma369.laimory.core.data.model.timeline.response.toDomain
import com.soma369.laimory.core.data.network.s3.PhotoMetaResolver
import com.soma369.laimory.core.data.network.s3.S3PhotoUploader
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionReporter
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class TimelineDraftRepositoryImpl
    @Inject
    constructor(
        private val photoMetaResolver: PhotoMetaResolver,
        private val remote: TimelineDraftRemoteDataSource,
        private val s3Uploader: S3PhotoUploader,
        private val json: Json,
        private val selectionReporter: DraftSourceItemSelectionReporter = DraftSourceItemSelectionReporter.NONE,
    ) : TimelineDraftRepository {
        override suspend fun uploadPhotos(clientPhotoUris: List<String>): List<String> {
            if (clientPhotoUris.isEmpty()) return emptyList()

            // presign 요청과 실제 PUT 이 정확히 같은 contentType/size 를 쓰도록 한 번만 산출한다.
            val metas = clientPhotoUris.map { photoMetaResolver.resolve(it) }
            val response =
                withinStepTimeout {
                    remote.requestPhotoUploads(
                        PhotoUploadCreateRequest(
                            photos = metas.map { PhotoUploadItem(contentType = it.contentType, size = it.size) },
                        ),
                    )
                }
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
            window: RecordDateWindow,
            items: List<SourceItem>,
            uploadedPhotoFilenames: Map<String, String>,
        ): DraftTaskHandle {
            val request =
                CreateDraftTaskRequest(
                    recordDate = recordDate.toString(),
                    recordAt = LocalDateTime.now(zone).toString(),
                    recordTimeZone = zone.id,
                    timelineWindow =
                        TimelineWindowDto(
                            startTime = LocalDateTime.ofInstant(window.start, zone).toString(),
                            endTime = LocalDateTime.ofInstant(window.end, zone).toString(),
                        ),
                    sourceItems = items.map { it.toSourceItemDto(json, uploadedPhotoFilenames[it.rawId]) },
                )
            if (selectionReporter.isEnabled) {
                val utf8ByteCount =
                    json
                        .encodeToString(CreateDraftTaskRequest.serializer(), request)
                        .encodeToByteArray()
                        .size
                selectionReporter.reportRequestSize(
                    sourceItemCount = request.sourceItems.size,
                    utf8ByteCount = utf8ByteCount,
                )
            }
            return withinStepTimeout { remote.createDraft(request) }.toDomain()
        }

        override suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot = remote.getDraftStatus(taskId).toDomain()

        /**
         * 초안 요청 한 단계(업로드 URL 발급 · 초안 생성 POST)에 상한을 건다. 둘 다 오가는 데이터가 작아 요청 전체가
         * 곧 진행 여부다. 사진 업로드는 큰 본문이라 여기 대신 S3 클라이언트의 읽기·쓰기 타임아웃(바이트 진행 기준)이 맡는다.
         *
         * 넘기면 [SocketTimeoutException] 으로 바꿔 던진다 — 코루틴 타임아웃은 취소로 읽혀 호출부의 실패 처리에 닿지
         * 않고, 분석은 이 예외로 무응답(TIMEOUT)을 가른다.
         */
        private suspend fun <T> withinStepTimeout(block: suspend () -> T): T =
            try {
                withTimeout(DRAFT_STEP_TIMEOUT) { block() }
            } catch (e: TimeoutCancellationException) {
                throw SocketTimeoutException("초안 요청이 ${DRAFT_STEP_TIMEOUT.inWholeSeconds}초 안에 끝나지 않았어요.")
            }

        private companion object {
            val DRAFT_STEP_TIMEOUT = 30.seconds
        }
    }
