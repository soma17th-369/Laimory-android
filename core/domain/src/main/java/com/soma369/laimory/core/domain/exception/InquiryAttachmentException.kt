package com.soma369.laimory.core.domain.exception

/**
 * 첨부 사진을 읽거나 올릴 형태로 바꾸지 못했다(삭제·권한 변경·디코딩 실패).
 *
 * 다시 보내도 같은 사진이면 또 실패하므로, 화면은 사진을 빼고 다시 고르게 안내한다.
 */
class InquiryAttachmentException(
    message: String,
) : ApiException(message)
