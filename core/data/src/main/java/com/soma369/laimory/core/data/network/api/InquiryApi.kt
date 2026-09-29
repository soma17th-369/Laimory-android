package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.inquiry.request.InquiryAttachmentUploadCreateRequest
import com.soma369.laimory.core.data.model.inquiry.request.InquiryCreateRequest
import com.soma369.laimory.core.data.model.inquiry.response.InquiryAttachmentUploadCreateResponse
import com.soma369.laimory.core.data.model.inquiry.response.InquiryDetailResponse
import com.soma369.laimory.core.data.model.inquiry.response.InquiryListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** 문의 접수와 내 문의 조회. 인증이 필요하다. 답변 본문은 없다 — 답변은 입력한 이메일로 온다. */
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

    /** 내 문의 최신 순 최대 50건. 제목·상태·시각만 싣는다. */
    @GET("inquiries")
    suspend fun getMyInquiries(): Response<ApiResponse<InquiryListResponse>>

    /** 내 문의 한 건. 없거나 남의 문의면 둘 다 404 `-404` 다. */
    @GET("inquiries/{inquiryId}")
    suspend fun getInquiry(
        @Path("inquiryId") inquiryId: Long,
    ): Response<ApiResponse<InquiryDetailResponse>>
}
