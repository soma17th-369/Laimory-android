package com.soma369.laimory.core.data.model.inquiry.response

import kotlinx.serialization.Serializable

/** 발급 결과. 요청한 첨부와 같은 순서다. */
@Serializable
data class InquiryAttachmentUploadCreateResponse(
    val uploads: List<InquiryAttachmentUploadResponse> = emptyList(),
)
