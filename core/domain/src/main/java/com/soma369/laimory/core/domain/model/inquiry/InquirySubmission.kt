package com.soma369.laimory.core.domain.model.inquiry

/**
 * 보낼 문의 한 건.
 *
 * @param email 답장 받을 주소. 회원 정보에 이메일이 없어 문의마다 받는다.
 * @param body 줄바꿈을 포함한 원문.
 * @param attachmentUris 첨부할 사진의 content Uri, 고른 순서대로. 최대 [InquiryInputRules.MAX_ATTACHMENTS] 장.
 */
data class InquirySubmission(
    val email: String,
    val body: String,
    val attachmentUris: List<String> = emptyList(),
) {
    val isValid: Boolean
        get() =
            InquiryInputRules.isValidEmail(email) &&
                InquiryInputRules.isValidBody(body) &&
                attachmentUris.size <= InquiryInputRules.MAX_ATTACHMENTS
}
