package com.soma369.laimory.core.data.model.inquiry.request

import kotlinx.serialization.Serializable

/**
 * 문의 접수 요청.
 *
 * @param attachmentFilenames 발급 응답의 `filename` 을 **요청 순서 그대로**. 서버는 S3 업로드 완료를
 *   확인하지 않으므로 PUT 이 모두 끝난 것만 담는다.
 */
@Serializable
data class InquiryCreateRequest(
    val email: String,
    val title: String,
    val description: String,
    val attachmentFilenames: List<String> = emptyList(),
)
