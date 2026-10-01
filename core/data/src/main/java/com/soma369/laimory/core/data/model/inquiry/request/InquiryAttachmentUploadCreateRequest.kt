package com.soma369.laimory.core.data.model.inquiry.request

import kotlinx.serialization.Serializable

/** 첨부 presigned PUT 발급 요청. 1~3장, 비어 있으면 400 이라 첨부가 없으면 부르지 않는다. */
@Serializable
data class InquiryAttachmentUploadCreateRequest(
    val attachments: List<InquiryAttachmentUploadItem>,
)
