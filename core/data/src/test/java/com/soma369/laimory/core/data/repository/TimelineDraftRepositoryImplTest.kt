package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.TimelineDraftRemoteDataSource
import com.soma369.laimory.core.data.model.timeline.request.CreateDraftTaskRequest
import com.soma369.laimory.core.data.model.timeline.request.PhotoUploadCreateRequest
import com.soma369.laimory.core.data.model.timeline.response.CreateDraftTaskResponse
import com.soma369.laimory.core.data.model.timeline.response.DraftTaskStatusResponse
import com.soma369.laimory.core.data.model.timeline.response.PhotoUploadCreateResponse
import com.soma369.laimory.core.data.model.timeline.response.PhotoUploadEntry
import com.soma369.laimory.core.data.network.s3.PhotoMeta
import com.soma369.laimory.core.data.network.s3.PhotoMetaResolver
import com.soma369.laimory.core.data.network.s3.S3PhotoUploader
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineDraftRepositoryImplTest {
    private val metaByUri =
        mapOf(
            "content://a" to PhotoMeta("image/jpeg", 111L),
            "content://b" to PhotoMeta("image/png", 222L),
        )

    private val resolver =
        object : PhotoMetaResolver {
            override fun resolve(clientPhotoUri: String): PhotoMeta = metaByUri.getValue(clientPhotoUri)
        }

    /** S3 PUT 에 넘어온 (uri, uploadUrl, contentType, size) 를 기록한다. */
    private class RecordingS3Uploader : S3PhotoUploader {
        val calls = mutableListOf<Triple<String, String, PhotoMeta>>()

        override suspend fun upload(
            clientPhotoUri: String,
            uploadUrl: String,
            contentType: String,
            size: Long,
        ) {
            calls += Triple(clientPhotoUri, uploadUrl, PhotoMeta(contentType, size))
        }
    }

    private class FakeRemote(
        private val uploadsResponse: PhotoUploadCreateResponse,
    ) : TimelineDraftRemoteDataSource {
        var lastPhotoUploadRequest: PhotoUploadCreateRequest? = null

        override suspend fun requestPhotoUploads(request: PhotoUploadCreateRequest): PhotoUploadCreateResponse {
            lastPhotoUploadRequest = request
            return uploadsResponse
        }

        override suspend fun createDraft(request: CreateDraftTaskRequest): CreateDraftTaskResponse = CreateDraftTaskResponse("t")

        override suspend fun getDraftStatus(taskId: String): DraftTaskStatusResponse = DraftTaskStatusResponse("PROCESSING", null)
    }

    @Test
    fun `uploadPhotos - presign 발급값과 S3 PUT 의 contentType·size 가 정확히 일치한다`() =
        runTest {
            val remote =
                FakeRemote(
                    PhotoUploadCreateResponse(
                        uploads =
                            listOf(
                                PhotoUploadEntry(filename = "srv-a.jpg", uploadUrl = "https://s3/a"),
                                PhotoUploadEntry(filename = "srv-b.png", uploadUrl = "https://s3/b"),
                            ),
                    ),
                )
            val s3 = RecordingS3Uploader()
            val repo = TimelineDraftRepositoryImpl(resolver, remote, s3, Json)

            val filenames = repo.uploadPhotos(listOf("content://a", "content://b"))

            // 발급 요청 photos[] 는 resolver 산출값 그대로.
            val requested = remote.lastPhotoUploadRequest!!.photos
            assertEquals(
                listOf("image/jpeg" to 111L, "image/png" to 222L),
                requested.map { it.contentType to it.size },
            )

            // S3 PUT 은 발급요청과 같은 contentType/size, 인덱스에 맞는 uploadUrl 로 나간다.
            assertEquals(
                listOf(
                    Triple("content://a", "https://s3/a", PhotoMeta("image/jpeg", 111L)),
                    Triple("content://b", "https://s3/b", PhotoMeta("image/png", 222L)),
                ),
                s3.calls,
            )

            // 반환 filename 은 발급 응답 순서 그대로(초안 payload 매핑의 기준).
            assertEquals(listOf("srv-a.jpg", "srv-b.png"), filenames)
        }

    @Test
    fun `uploadPhotos - 사진이 없으면 발급도 업로드도 하지 않는다`() =
        runTest {
            val remote = FakeRemote(PhotoUploadCreateResponse(uploads = emptyList()))
            val s3 = RecordingS3Uploader()
            val repo = TimelineDraftRepositoryImpl(resolver, remote, s3, Json)

            val filenames = repo.uploadPhotos(emptyList())

            assertEquals(emptyList<String>(), filenames)
            assertNull(remote.lastPhotoUploadRequest)
            assertEquals(0, s3.calls.size)
        }
}
