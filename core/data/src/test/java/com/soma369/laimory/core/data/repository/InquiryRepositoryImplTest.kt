package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.InquiryRemoteDataSource
import com.soma369.laimory.core.data.inquiry.InquiryAttachmentPreparer
import com.soma369.laimory.core.data.inquiry.PreparedInquiryAttachment
import com.soma369.laimory.core.data.model.inquiry.request.InquiryAttachmentUploadCreateRequest
import com.soma369.laimory.core.data.model.inquiry.request.InquiryAttachmentUploadItem
import com.soma369.laimory.core.data.model.inquiry.request.InquiryCreateRequest
import com.soma369.laimory.core.data.model.inquiry.response.InquiryAttachmentUploadCreateResponse
import com.soma369.laimory.core.data.model.inquiry.response.InquiryAttachmentUploadResponse
import com.soma369.laimory.core.data.network.s3.S3PhotoUploader
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.inquiry.InquirySubmission
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

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
            repository.submit(InquirySubmission(email = "user@example.com", body = "문의"))

            assertNull(remote.uploadRequest)
            assertEquals(InquiryCreateRequest("user@example.com", "문의", emptyList()), remote.created)
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

    private fun submission(vararg uris: String) = InquirySubmission(email = "user@example.com", body = "문의", attachmentUris = uris.toList())

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
