package com.soma369.laimory.core.data.inquiry

/** 고른 사진을 서버가 받는 형식(JPG · 장당 5MB 이하)으로 바꾼다. */
interface InquiryAttachmentPreparer {
    /** 실패하면 [com.soma369.laimory.core.domain.exception.InquiryAttachmentException]. */
    suspend fun prepare(sourceUri: String): PreparedInquiryAttachment
}
