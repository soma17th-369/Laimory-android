package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.InquiryRemoteDataSource
import com.soma369.laimory.core.data.inquiry.InquiryAttachmentPreparer
import com.soma369.laimory.core.data.inquiry.PreparedInquiryAttachment
import com.soma369.laimory.core.data.model.inquiry.request.InquiryAttachmentUploadCreateRequest
import com.soma369.laimory.core.data.model.inquiry.request.InquiryAttachmentUploadItem
import com.soma369.laimory.core.data.model.inquiry.request.InquiryCreateRequest
import com.soma369.laimory.core.data.model.inquiry.response.InquiryAttachmentUploadCreateResponse
import com.soma369.laimory.core.data.model.inquiry.response.InquiryAttachmentUploadResponse
import com.soma369.laimory.core.data.model.inquiry.response.InquiryDetailResponse
import com.soma369.laimory.core.data.model.inquiry.response.InquiryListResponse
import com.soma369.laimory.core.data.model.inquiry.response.InquirySummaryResponse
import com.soma369.laimory.core.data.network.s3.S3PhotoUploader
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.InquiryNotFoundException
import com.soma369.laimory.core.domain.model.inquiry.InquiryStatus
import com.soma369.laimory.core.domain.model.inquiry.InquirySubmission
import com.soma369.laimory.core.domain.model.inquiry.InquirySummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDateTime

class InquiryRepositoryImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val remote = FakeInquiryRemoteDataSource()
    private val uploader = RecordingS3PhotoUploader()
    private val preparer = FakePreparer()
    private val repository = InquiryRepositoryImpl(remote, preparer, uploader)

    @Test
    fun `첨부가 없으면 발급을 부르지 않고 바로 접수한다`() =
        runTest {
            // 서버는 빈 첨부 발급을 400 으로 돌려준다.
            repository.submit(InquirySubmission(email = "user@example.com", title = "제목", description = "문의"))

            assertNull(remote.uploadRequest)
            assertEquals(InquiryCreateRequest("user@example.com", "제목", "문의", emptyList()), remote.created)
        }

    @Test
    fun `준비한 첨부의 형식·크기로 발급받아 같은 순서로 올리고 파일명을 그 순서로 접수한다`() =
        runTest {
            repository.submit(submission("content://a", "content://b"))

            assertEquals(
                InquiryAttachmentUploadCreateRequest(
                    listOf(InquiryAttachmentUploadItem("image/jpeg", 10), InquiryAttachmentUploadItem("image/jpeg", 20)),
                ),
                remote.uploadRequest,
            )
            assertEquals(listOf("https://s3/0", "https://s3/1"), uploader.uploadedUrls)
            assertEquals(listOf(10L, 20L), uploader.uploadedSizes)
            assertEquals(listOf("f0.jpg", "f1.jpg"), remote.created?.attachmentFilenames)
        }

    @Test
    fun `올리기가 하나라도 실패하면 접수하지 않는다`() =
        runTest {
            // 서버는 접수 때 업로드 완료를 확인하지 않는다. 그대로 접수하면 보이지 않는 첨부가 달린다.
            uploader.failAt = 1

            val result = runCatching { repository.submit(submission("content://a", "content://b")) }

            assertTrue(result.exceptionOrNull() is ApiException.NetworkException)
            assertNull(remote.created)
        }

    @Test
    fun `발급 개수가 요청과 다르면 짝을 지을 수 없어 멈춘다`() =
        runTest {
            remote.uploadCount = 1

            val result = runCatching { repository.submit(submission("content://a", "content://b")) }

            assertTrue(result.isFailure)
            assertTrue(uploader.uploadedUrls.isEmpty())
            assertNull(remote.created)
        }

    @Test
    fun `성공해도 실패해도 준비한 임시 파일을 지운다`() =
        runTest {
            repository.submit(submission("content://a"))
            uploader.failAt = 0
            runCatching { repository.submit(submission("content://b")) }

            assertEquals(2, preparer.files.size)
            preparer.files.forEach { assertFalse(it.name, it.exists()) }
        }

    @Test
    fun `내 문의 목록을 서버 순서대로 옮기고 상태·시각을 읽는다`() =
        runTest {
            remote.list =
                listOf(
                    InquirySummaryResponse(2, "사진이 안 올라가요", "RECEIVED", "2026-09-29T10:00:00", null),
                    InquirySummaryResponse(1, "타임라인이 비어요", "ANSWERED", "2026-09-20T09:00:00.123", "2026-09-21T14:00:00"),
                )

            assertEquals(
                listOf(
                    InquirySummary(2, "사진이 안 올라가요", InquiryStatus.RECEIVED, LocalDateTime.of(2026, 9, 29, 10, 0), null),
                    InquirySummary(
                        1,
                        "타임라인이 비어요",
                        InquiryStatus.ANSWERED,
                        LocalDateTime.of(2026, 9, 20, 9, 0, 0, 123_000_000),
                        LocalDateTime.of(2026, 9, 21, 14, 0),
                    ),
                ),
                repository.getMyInquiries(),
            )
        }

    @Test
    fun `모르는 상태는 확인 중으로, 접수 시각을 읽지 못한 문의는 그 건만 버린다`() =
        runTest {
            // 답하지 않은 문의를 답했다고 말하는 쪽이 더 나쁘다.
            remote.list =
                listOf(
                    InquirySummaryResponse(3, "새 상태", "ON_HOLD", "2026-09-29T10:00:00", null),
                    InquirySummaryResponse(2, "시각 깨짐", "RECEIVED", "2026-09-29T10:00:00+09:00", null),
                )

            val inquiries = repository.getMyInquiries()

            assertEquals(listOf(3L), inquiries.map { it.id })
            assertEquals(InquiryStatus.RECEIVED, inquiries.single().status)
        }

    @Test
    fun `상세는 보낸 내용과 첨부 주소를 순서대로 옮긴다`() =
        runTest {
            remote.detail =
                InquiryDetailResponse(
                    inquiryId = 7,
                    title = "제목",
                    status = "ANSWERED",
                    email = "user@example.com",
                    description = "내용\n두 줄",
                    attachmentUrls = listOf("https://cdn/b.jpg", "https://cdn/a.jpg"),
                    createdAt = "2026-09-29T10:00:00",
                    answeredAt = "2026-09-30T14:00:00",
                )

            val detail = repository.getInquiry(7)

            assertEquals(listOf("https://cdn/b.jpg", "https://cdn/a.jpg"), detail.attachmentUrls)
            assertEquals(InquiryStatus.ANSWERED, detail.status)
            assertEquals("내용\n두 줄", detail.description)
        }

    @Test
    fun `없거나 남의 문의인 404 는 찾을 수 없는 문의로 올린다`() =
        runTest {
            remote.detailFailure = ApiException.ClientException(errorCode = -404, rawCode = 404)

            val result = runCatching { repository.getInquiry(7) }

            assertTrue(result.exceptionOrNull() is InquiryNotFoundException)
        }

    private fun submission(vararg uris: String) =
        InquirySubmission(
            email = "user@example.com",
            title = "제목",
            description = "문의",
            attachmentUris = uris.toList(),
        )

    private inner class FakePreparer : InquiryAttachmentPreparer {
        val files = mutableListOf<File>()

        override suspend fun prepare(sourceUri: String): PreparedInquiryAttachment {
            val file = tempFolder.newFile()
            files += file
            return PreparedInquiryAttachment(file = file, contentType = "image/jpeg", size = 10L * files.size)
        }
    }

    private class FakeInquiryRemoteDataSource : InquiryRemoteDataSource {
        var uploadRequest: InquiryAttachmentUploadCreateRequest? = null
        var created: InquiryCreateRequest? = null
        var uploadCount: Int? = null

        override suspend fun createAttachmentUploads(request: InquiryAttachmentUploadCreateRequest): InquiryAttachmentUploadCreateResponse {
            uploadRequest = request
            val count = uploadCount ?: request.attachments.size
            return InquiryAttachmentUploadCreateResponse(
                List(count) { InquiryAttachmentUploadResponse(filename = "f$it.jpg", uploadUrl = "https://s3/$it") },
            )
        }

        override suspend fun createInquiry(request: InquiryCreateRequest) {
            created = request
        }

        var list: List<InquirySummaryResponse> = emptyList()
        var detail: InquiryDetailResponse? = null
        var detailFailure: ApiException? = null

        override suspend fun getMyInquiries(): InquiryListResponse = InquiryListResponse(list)

        override suspend fun getInquiry(inquiryId: Long): InquiryDetailResponse {
            detailFailure?.let { throw it }
            return checkNotNull(detail)
        }
    }

    private class RecordingS3PhotoUploader : S3PhotoUploader {
        val uploadedUrls = mutableListOf<String>()
        val uploadedSizes = mutableListOf<Long>()
        var failAt: Int? = null

        override suspend fun upload(
            clientPhotoUri: String,
            uploadUrl: String,
            contentType: String,
            size: Long,
        ) {
            if (failAt == uploadedUrls.size) throw ApiException.NetworkException()
            uploadedUrls += uploadUrl
            uploadedSizes += size
        }
    }
}
