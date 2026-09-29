package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.inquiry.request.InquiryAttachmentUploadCreateRequest
import com.soma369.laimory.core.data.model.inquiry.request.InquiryCreateRequest
import com.soma369.laimory.core.data.model.inquiry.response.InquiryAttachmentUploadCreateResponse
import com.soma369.laimory.core.data.model.inquiry.response.InquiryDetailResponse
import com.soma369.laimory.core.data.model.inquiry.response.InquiryListResponse
import com.soma369.laimory.core.data.network.api.InquiryApi
import com.soma369.laimory.core.data.network.safeApiCall
import com.soma369.laimory.core.data.network.safeApiCallUnit
import javax.inject.Inject

class InquiryRemoteDataSourceImpl
    @Inject
    constructor(
        private val inquiryApi: InquiryApi,
    ) : InquiryRemoteDataSource {
        override suspend fun createAttachmentUploads(request: InquiryAttachmentUploadCreateRequest): InquiryAttachmentUploadCreateResponse =
            safeApiCall { inquiryApi.createAttachmentUploads(request) }

        // 접수 성공은 201 에 envelope body 가 비어 있다.
        override suspend fun createInquiry(request: InquiryCreateRequest) = safeApiCallUnit { inquiryApi.createInquiry(request) }

        override suspend fun getMyInquiries(): InquiryListResponse = safeApiCall { inquiryApi.getMyInquiries() }

        override suspend fun getInquiry(inquiryId: Long): InquiryDetailResponse = safeApiCall { inquiryApi.getInquiry(inquiryId) }
    }
