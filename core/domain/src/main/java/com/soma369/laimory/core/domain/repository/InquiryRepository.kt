package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.inquiry.InquiryDetail
import com.soma369.laimory.core.domain.model.inquiry.InquirySubmission
import com.soma369.laimory.core.domain.model.inquiry.InquirySummary

/**
 * 문의 접수와 내 문의 조회. 답변 본문은 서버에 저장되지 않고 관리자가 입력한 이메일로 직접 회신한다 —
 * 앱이 보여 주는 것은 내가 보낸 내용과 처리 상태뿐이다.
 */
interface InquiryRepository {
    /**
     * 첨부를 올린 뒤 문의를 접수한다. 반환하면 접수가 끝난 것이다.
     *
     * 같은 내용을 다시 보내면 서버는 새 문의로 또 받는다(멱등 키 없음). 중복을 막는 것은 호출부 몫이다.
     */
    suspend fun submit(submission: InquirySubmission)

    /** 내 문의, 최신 순 최대 50건. 없으면 빈 목록이다(오류가 아니다). */
    suspend fun getMyInquiries(): List<InquirySummary>

    /** 내 문의 한 건. 없거나 내 것이 아니면 [com.soma369.laimory.core.domain.exception.InquiryNotFoundException]. */
    suspend fun getInquiry(inquiryId: Long): InquiryDetail
}
