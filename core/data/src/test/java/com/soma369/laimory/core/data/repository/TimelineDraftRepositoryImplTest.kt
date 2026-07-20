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
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
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

        override suspend fun requestPhotoUploads(request: PhotoUploadCreateRequest): PhotoUploadCreateResponse {
            lastPhotoUploadRequest = request
            return uploadsResponse
        }

        var lastDraftRequest: CreateDraftTaskRequest? = null

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

    private fun notificationItem(zone: ZoneId): SourceItem {
        val startAt = LocalDate.of(2026, 7, 8).atTime(14, 0).atZone(zone).toInstant()
        return SourceItem(
            rawId = "raw-n1",
            startAt = startAt,
            endAt = null,
            timeZoneId = zone,
            payload =
                NotificationPayload(
                    appName = "카카오톡",
                    packageName = "com.kakao.talk",
                    title = "제목",
                    text = null,
                    collectReason = NotificationPayload.CollectReason.ALL,
                ),
            sourceName = SourceName.NOTIFICATION_LISTENER,
            sourceKey = "k1",
            collectedAt = startAt,
        )
    }

    @Test
    fun `createDraft - recordDate 와 선택 날짜 창 window 를 명시 전송하고 recordAt 은 실제 시각이다`() =
        runTest {
            val remote = FakeRemote(PhotoUploadCreateResponse(uploads = emptyList()))
            val repo = TimelineDraftRepositoryImpl(resolver, remote, RecordingS3Uploader(), Json)
            val zone = ZoneId.of("Asia/Seoul")
            val date = LocalDate.of(2026, 7, 8)

            val before = LocalDateTime.now(zone)
            repo.createDraft(date, zone, listOf(notificationItem(zone)), emptyMap())
            val after = LocalDateTime.now(zone)

            val request = remote.lastDraftRequest!!
            // recordDate(선택 날짜)가 단일 권위 — 서버 파생 없이 그대로 실린다.
            assertEquals("2026-07-08", request.recordDate)
            assertEquals("Asia/Seoul", request.recordTimeZone)
            // window 는 선택 날짜의 달력 하루 — 아이템 시각(14:00) min/max 와 무관하다.
            assertEquals("2026-07-08T00:00", request.timelineWindow.startTime)
            assertEquals("2026-07-09T00:00", request.timelineWindow.endTime)
            // recordAt 은 조작된 자정(recordDate.atStartOfDay())이 아니라 호출 시점의 실제 시각이다.
            val recordAt = LocalDateTime.parse(request.recordAt)
            assertTrue(!recordAt.isBefore(before) && !recordAt.isAfter(after))
        }

    @Test
    fun `createDraft - 자정이 없는 날은 창의 실제 로컬 경계를 변형 없이 보낸다`() =
        runTest {
            // America/Sao_Paulo 2017-10-15: DST 시작으로 자정이 없어 하루가 01:00 에 시작한다.
            // 서버는 calendar-day shape 를 재검증하지 않으므로 실제 경계를 그대로 보낸다.
            val remote = FakeRemote(PhotoUploadCreateResponse(uploads = emptyList()))
            val repo = TimelineDraftRepositoryImpl(resolver, remote, RecordingS3Uploader(), Json)
            val zone = ZoneId.of("America/Sao_Paulo")

            repo.createDraft(LocalDate.of(2017, 10, 15), zone, listOf(notificationItem(zone)), emptyMap())

            val window = remote.lastDraftRequest!!.timelineWindow
            assertEquals("2017-10-15T01:00", window.startTime)
            assertEquals("2017-10-16T00:00", window.endTime)
        }

    @Test
    fun `createDraft - 요청 JSON 필드명이 서버 OpenAPI 계약과 일치한다`() =
        runTest {
            val remote = FakeRemote(PhotoUploadCreateResponse(uploads = emptyList()))
            val repo = TimelineDraftRepositoryImpl(resolver, remote, RecordingS3Uploader(), Json)
            val zone = ZoneId.of("Asia/Seoul")

            repo.createDraft(LocalDate.of(2026, 7, 8), zone, listOf(notificationItem(zone)), emptyMap())

            val tree = Json.encodeToJsonElement(CreateDraftTaskRequest.serializer(), remote.lastDraftRequest!!).jsonObject
            assertEquals(
                setOf("recordDate", "recordAt", "recordTimeZone", "timelineWindow", "sourceItems"),
                tree.keys,
            )
            assertEquals(setOf("startTime", "endTime"), tree.getValue("timelineWindow").jsonObject.keys)
        }
}
