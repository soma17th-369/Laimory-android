package com.soma369.laimory.core.domain.model.inquiry

import java.time.LocalDateTime

/**
 * 내 문의 목록의 한 건. 제목으로 훑는다 — 내용·답장 이메일·첨부는 [InquiryDetail] 에만 있다.
 *
 * @param createdAt 접수 시각(한국 벽시계).
 * @param answeredAt 답변 완료 표시 시각. [InquiryStatus.RECEIVED] 면 `null`.
 */
data class InquirySummary(
    val id: Long,
    val title: String,
    val status: InquiryStatus,
    val createdAt: LocalDateTime,
    val answeredAt: LocalDateTime?,
)
