package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.inquiry.InquirySubmission

/**
 * 문의 접수. 앱에는 "내 문의" 조회가 없다 — 답변은 서버에 저장되지 않고 관리자가 입력한 이메일로
 * 직접 회신한다.
 */
interface InquiryRepository {
    /**
     * 첨부를 올린 뒤 문의를 접수한다. 반환하면 접수가 끝난 것이다.
     *
     * 같은 내용을 다시 보내면 서버는 새 문의로 또 받는다(멱등 키 없음). 중복을 막는 것은 호출부 몫이다.
     */
    suspend fun submit(submission: InquirySubmission)
}
