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
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TimelineDraftRepositoryImplTest {
    private val metaByUri =
        mapOf(
            "content://a" to PhotoMeta("image/jpeg", 111L),
            "content://b" to PhotoMeta("image/png", 222L),
        )

    private val resolver =
        object : PhotoMetaResolver {
            override suspend fun resolve(clientPhotoUri: String): PhotoMeta = metaByUri.getValue(clientPhotoUri)
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
        var lastDraftRequest: CreateDraftTaskRequest? = null

        override suspend fun requestPhotoUploads(request: PhotoUploadCreateRequest): PhotoUploadCreateResponse {
            lastPhotoUploadRequest = request
            return uploadsResponse
        }

        override suspend fun createDraft(request: CreateDraftTaskRequest): CreateDraftTaskResponse {
            lastDraftRequest = request
            return CreateDraftTaskResponse("t")
        }

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

    @Test
    fun `createDraft - recordDate와 선택 창을 로컬 datetime 계약으로 전송한다`() =
        runTest {
            val remote = FakeRemote(PhotoUploadCreateResponse(uploads = emptyList()))
            val repo = TimelineDraftRepositoryImpl(resolver, remote, RecordingS3Uploader(), Json)
            val zone = ZoneId.of("Asia/Seoul")
            val date = LocalDate.of(2026, 7, 8)
            val window =
                RecordDateWindow(
                    start = date.atTime(9, 30).atZone(zone).toInstant(),
                    end = date.plusDays(1).atTime(2, 15).atZone(zone).toInstant(),
                )
            val before = LocalDateTime.now(zone).minusSeconds(1)

            repo.createDraft(date, zone, window, emptyList(), emptyMap())

            val request = remote.lastDraftRequest!!
            val after = LocalDateTime.now(zone).plusSeconds(1)
            assertEquals("2026-07-08", request.recordDate)
            assertEquals("Asia/Seoul", request.recordTimeZone)
            assertEquals("2026-07-08T09:30", request.timelineWindow.startTime)
            assertEquals("2026-07-09T02:15", request.timelineWindow.endTime)
            val recordAt = LocalDateTime.parse(request.recordAt)
            assertTrue(!recordAt.isBefore(before) && !recordAt.isAfter(after))

            val encoded = Json.encodeToString(CreateDraftTaskRequest.serializer(), request)
            assertTrue(encoded.contains("\"recordDate\":\"2026-07-08\""))
            assertTrue(encoded.contains("\"recordAt\":"))
            assertTrue(encoded.contains("\"recordTimeZone\":\"Asia/Seoul\""))
            assertTrue(encoded.contains("\"timelineWindow\":{"))
            assertTrue(encoded.contains("\"startTime\":\"2026-07-08T09:30\""))
            assertTrue(encoded.contains("\"endTime\":\"2026-07-09T02:15\""))
        }

    @Test
    fun `createDraft - DST gap에서 보정된 실제 로컬 시각을 창으로 전송한다`() =
        runTest {
            val remote = FakeRemote(PhotoUploadCreateResponse(uploads = emptyList()))
            val repo = TimelineDraftRepositoryImpl(resolver, remote, RecordingS3Uploader(), Json)
            val zone = ZoneId.of("America/New_York")
            val date = LocalDate.of(2026, 3, 8)
            // 02:30은 DST 전환으로 존재하지 않아 atZone이 실제 시각 03:30으로 보정한다.
            val window =
                RecordDateWindow(
                    start = date.atTime(2, 30).atZone(zone).toInstant(),
                    end = date.atTime(4, 0).atZone(zone).toInstant(),
                )

            repo.createDraft(date, zone, window, emptyList(), emptyMap())

            assertEquals("2026-03-08T03:30", remote.lastDraftRequest!!.timelineWindow.startTime)
            assertEquals("2026-03-08T04:00", remote.lastDraftRequest!!.timelineWindow.endTime)
        }
}
