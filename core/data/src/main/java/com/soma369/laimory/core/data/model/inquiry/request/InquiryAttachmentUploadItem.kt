package com.soma369.laimory.core.data.model.inquiry.request

import kotlinx.serialization.Serializable

/** 첨부 한 장의 업로드 메타. [size] 는 presigned PUT 의 `Content-Length` 에 묶인다 — PUT 바이트와 같아야 한다. */
@Serializable
data class InquiryAttachmentUploadItem(
    val contentType: String,
    val size: Long,
)
