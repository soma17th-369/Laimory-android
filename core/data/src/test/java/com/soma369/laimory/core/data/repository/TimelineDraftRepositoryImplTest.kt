package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.TimelineDraftRemoteDataSource
import com.soma369.laimory.core.data.model.timeline.request.CreateDraftTaskRequest
import com.soma369.laimory.core.data.model.timeline.request.PhotoUploadCreateRequest
import com.soma369.laimory.core.data.model.timeline.response.CreateDraftTaskResponse
import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineResponse
import com.soma369.laimory.core.data.model.timeline.response.DraftTaskStatusResponse
import com.soma369.laimory.core.data.model.timeline.response.PhotoUploadCreateResponse
import com.soma369.laimory.core.data.model.timeline.response.PhotoUploadEntry
import com.soma369.laimory.core.data.network.s3.PhotoMeta
import com.soma369.laimory.core.data.network.s3.PhotoMetaResolver
import com.soma369.laimory.core.data.network.s3.S3PhotoUploader
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionReport
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionReporter
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatus
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
        private val statusResponse: DraftTaskStatusResponse = DraftTaskStatusResponse(status = "PROCESSING"),
        /** 채우면 응답 전에 그만큼 멈춘다. 무응답을 만든다. */
        private val responseDelay: Duration = Duration.ZERO,
    ) : TimelineDraftRemoteDataSource {
        var lastPhotoUploadRequest: PhotoUploadCreateRequest? = null
        var lastDraftRequest: CreateDraftTaskRequest? = null

        override suspend fun requestPhotoUploads(request: PhotoUploadCreateRequest): PhotoUploadCreateResponse {
            lastPhotoUploadRequest = request
            delay(responseDelay)
            return uploadsResponse
        }

        override suspend fun createDraft(request: CreateDraftTaskRequest): CreateDraftTaskResponse {
            lastDraftRequest = request
            delay(responseDelay)
            return CreateDraftTaskResponse("t")
        }

        override suspend fun getDraftStatus(taskId: String): DraftTaskStatusResponse = statusResponse
    }

    private class RecordingSelectionReporter : DraftSourceItemSelectionReporter {
        override val isEnabled: Boolean = true
        var reportedSourceItemCount: Int? = null
        var reportedUtf8ByteCount: Int? = null

        override fun reportSelection(report: DraftSourceItemSelectionReport) = Unit

        override fun reportRequestSize(
            sourceItemCount: Int,
            utf8ByteCount: Int,
        ) {
            reportedSourceItemCount = sourceItemCount
            reportedUtf8ByteCount = utf8ByteCount
        }
    }

    @Test
    fun `getDraftStatus - SUCCESS 결과 식별자를 Domain까지 전달한다`() =
        runTest {
            val remote =
                FakeRemote(
                    uploadsResponse = PhotoUploadCreateResponse(uploads = emptyList()),
                    statusResponse =
                        DraftTaskStatusResponse(
                            status = "SUCCESS",
                            result =
                                DailyTimelineResponse(
                                    dailyRecordId = 31L,
                                    recordDate = "2026-07-08",
                                    events = emptyList(),
                                ),
                        ),
                )
            val repo = TimelineDraftRepositoryImpl(resolver, remote, RecordingS3Uploader(), Json)

            val snapshot = repo.getDraftStatus("task-1")

            assertEquals(DraftTaskStatus.SUCCESS, snapshot.status)
            assertEquals(31L, snapshot.result?.dailyRecordId)
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

    @Test
    fun `createDraft - debug 측정 포트에 sourceItems 수와 직렬화 byte를 전달한다`() =
        runTest {
            val remote = FakeRemote(PhotoUploadCreateResponse(uploads = emptyList()))
            val reporter = RecordingSelectionReporter()
            val repo =
                TimelineDraftRepositoryImpl(
                    photoMetaResolver = resolver,
                    remote = remote,
                    s3Uploader = RecordingS3Uploader(),
                    json = Json,
                    selectionReporter = reporter,
                )
            val zone = ZoneId.of("Asia/Seoul")
            val date = LocalDate.of(2026, 7, 8)
            val window = RecordDateWindow.ofDate(date, zone)

            repo.createDraft(date, zone, window, emptyList(), emptyMap())

            val encoded =
                Json.encodeToString(
                    CreateDraftTaskRequest.serializer(),
                    remote.lastDraftRequest!!,
                )
            assertEquals(0, reporter.reportedSourceItemCount)
            assertEquals(encoded.encodeToByteArray().size, reporter.reportedUtf8ByteCount)
        }

    @Test
    fun `uploadPhotos - 업로드 URL 발급이 30초 안에 끝나지 않으면 무응답으로 끊는다`() =
        runTest {
            val remote =
                FakeRemote(
                    PhotoUploadCreateResponse(uploads = listOf(PhotoUploadEntry(filename = "f", uploadUrl = "u"))),
                    responseDelay = 31.seconds,
                )
            val repository = TimelineDraftRepositoryImpl(resolver, remote, RecordingS3Uploader(), Json)

            val error = runCatching { repository.uploadPhotos(listOf("content://a")) }.exceptionOrNull()

            assertTrue(error is SocketTimeoutException)
        }

    @Test
    fun `createDraft - 초안 생성 요청이 30초 안에 끝나지 않으면 무응답으로 끊는다`() =
        runTest {
            val remote = FakeRemote(PhotoUploadCreateResponse(uploads = emptyList()), responseDelay = 31.seconds)
            val repository = TimelineDraftRepositoryImpl(resolver, remote, RecordingS3Uploader(), Json)

            val error =
                runCatching {
                    repository.createDraft(
                        recordDate = LocalDate.of(2026, 10, 7),
                        zone = ZoneId.of("Asia/Seoul"),
                        window = window(LocalDate.of(2026, 10, 7), ZoneId.of("Asia/Seoul")),
                        items = emptyList(),
                        uploadedPhotoFilenames = emptyMap(),
                    )
                }.exceptionOrNull()

            assertTrue(error is SocketTimeoutException)
        }

    @Test
    fun `createDraft - 30초 안에 오면 그대로 받는다`() =
        runTest {
            val remote = FakeRemote(PhotoUploadCreateResponse(uploads = emptyList()), responseDelay = 29.seconds)
            val repository = TimelineDraftRepositoryImpl(resolver, remote, RecordingS3Uploader(), Json)

            val handle =
                repository.createDraft(
                    recordDate = LocalDate.of(2026, 10, 7),
                    zone = ZoneId.of("Asia/Seoul"),
                    window = window(LocalDate.of(2026, 10, 7), ZoneId.of("Asia/Seoul")),
                    items = emptyList(),
                    uploadedPhotoFilenames = emptyMap(),
                )

            assertEquals("t", handle.taskId)
        }

    private fun window(
        date: LocalDate,
        zone: ZoneId,
    ): RecordDateWindow =
        RecordDateWindow(
            start = date.atTime(6, 0).atZone(zone).toInstant(),
            end = date.plusDays(1).atTime(6, 0).atZone(zone).toInstant(),
        )
}
