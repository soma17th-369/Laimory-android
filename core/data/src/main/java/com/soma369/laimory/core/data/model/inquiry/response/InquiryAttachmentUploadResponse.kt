package com.soma369.laimory.core.data.model.inquiry.response

import kotlinx.serialization.Serializable

/** 첨부 한 장의 발급 결과. [filename] 은 접수 요청에 그대로 돌려보내는 식별자다. */
@Serializable
data class InquiryAttachmentUploadResponse(
    val filename: String,
    val uploadUrl: String,
)
