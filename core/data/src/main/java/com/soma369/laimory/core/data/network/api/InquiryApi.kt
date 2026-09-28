package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.inquiry.request.InquiryAttachmentUploadCreateRequest
import com.soma369.laimory.core.data.model.inquiry.request.InquiryCreateRequest
import com.soma369.laimory.core.data.model.inquiry.response.InquiryAttachmentUploadCreateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** 문의 접수. 인증이 필요하다. 접수만 있고 조회는 없다 — 답변은 입력한 이메일로 온다. */
interface InquiryApi {
    /** 첨부 presigned PUT URL 발급. 초과 3장 `-1004`, 장당 5MB 초과 `-1005`, JPG/PNG/WebP 외 `-1007`. */
    @POST("inquiries/attachment-uploads")
    suspend fun createAttachmentUploads(
        @Body request: InquiryAttachmentUploadCreateRequest,
    ): Response<ApiResponse<InquiryAttachmentUploadCreateResponse>>

    /** 접수. 201 이 곧 완료이고 본문이 없다. */
    @POST("inquiries")
    suspend fun createInquiry(
        @Body request: InquiryCreateRequest,
    ): Response<ApiResponse<Unit>>
}
