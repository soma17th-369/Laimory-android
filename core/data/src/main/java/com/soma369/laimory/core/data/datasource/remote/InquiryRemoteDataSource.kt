package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.inquiry.request.InquiryAttachmentUploadCreateRequest
import com.soma369.laimory.core.data.model.inquiry.request.InquiryCreateRequest
import com.soma369.laimory.core.data.model.inquiry.response.InquiryAttachmentUploadCreateResponse

interface InquiryRemoteDataSource {
    suspend fun createAttachmentUploads(request: InquiryAttachmentUploadCreateRequest): InquiryAttachmentUploadCreateResponse

    suspend fun createInquiry(request: InquiryCreateRequest)
}
