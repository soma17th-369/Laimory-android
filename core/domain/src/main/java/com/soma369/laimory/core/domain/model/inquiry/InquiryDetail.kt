package com.soma369.laimory.core.domain.model.inquiry

import java.time.LocalDateTime

/**
 * 내 문의 한 건의 보낸 내용과 처리 상태. 답변 본문은 서버에 없다 — 답장은 [email] 로 간다.
 *
 * @param attachmentUrls 첨부 사진 주소, 보낸 순서대로. 만료가 없는 CDN 주소다.
 */
data class InquiryDetail(
    val id: Long,
    val title: String,
    val status: InquiryStatus,
    val email: String,
    val description: String,
    val attachmentUrls: List<String>,
    val createdAt: LocalDateTime,
    val answeredAt: LocalDateTime?,
)
