package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.InquiryRemoteDataSource
import com.soma369.laimory.core.data.inquiry.InquiryAttachmentPreparer
import com.soma369.laimory.core.data.inquiry.PreparedInquiryAttachment
import com.soma369.laimory.core.data.model.inquiry.request.InquiryAttachmentUploadCreateRequest
import com.soma369.laimory.core.data.model.inquiry.request.InquiryAttachmentUploadItem
import com.soma369.laimory.core.data.model.inquiry.request.InquiryCreateRequest
import com.soma369.laimory.core.data.network.s3.S3PhotoUploader
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.inquiry.InquirySubmission
import com.soma369.laimory.core.domain.repository.InquiryRepository
import javax.inject.Inject

/**
 * 첨부 준비 → presign 발급 → S3 PUT → 접수 순서로 보낸다.
 *
 * 서버는 접수 때 S3 업로드 완료를 확인하지 않는다. 그래서 PUT 이 하나라도 실패하면 접수하지 않는다 —
 * 보이지 않는 첨부가 달린 문의가 되기 때문이다. 이미 올라간 첨부는 서버가 탈퇴 때 prefix 째 지운다.
 * 다시 보낼 때는 발급부터 처음부터 한다.
 */
class InquiryRepositoryImpl
    @Inject
    constructor(
        private val remoteDataSource: InquiryRemoteDataSource,
        private val attachmentPreparer: InquiryAttachmentPreparer,
        private val s3PhotoUploader: S3PhotoUploader,
    ) : InquiryRepository {
        override suspend fun submit(submission: InquirySubmission) {
            val prepared = mutableListOf<PreparedInquiryAttachment>()
            try {
                submission.attachmentUris.forEach { prepared += attachmentPreparer.prepare(it) }
                val filenames = upload(prepared)
                remoteDataSource.createInquiry(
                    InquiryCreateRequest(
                        email = submission.email,
                        body = submission.body,
                        attachmentFilenames = filenames,
                    ),
                )
            } finally {
                prepared.forEach { it.file.delete() }
            }
        }

        /** 발급이 첨부와 같은 순서로 온다는 계약에 기댄다. 개수가 다르면 짝을 지을 수 없어 멈춘다. */
        private suspend fun upload(prepared: List<PreparedInquiryAttachment>): List<String> {
            if (prepared.isEmpty()) return emptyList()
            val uploads =
                remoteDataSource
                    .createAttachmentUploads(
                        InquiryAttachmentUploadCreateRequest(
                            prepared.map { InquiryAttachmentUploadItem(contentType = it.contentType, size = it.size) },
                        ),
                    ).uploads
            if (uploads.size != prepared.size) throw ApiException.UnknownException("첨부 발급 개수가 요청과 다릅니다")
            return uploads.zip(prepared).map { (upload, attachment) ->
                s3PhotoUploader.upload(
                    clientPhotoUri = attachment.uri,
                    uploadUrl = upload.uploadUrl,
                    contentType = attachment.contentType,
                    size = attachment.size,
                )
                upload.filename
            }
        }
    }
