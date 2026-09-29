package com.soma369.laimory.core.data.model.inquiry.response

import kotlinx.serialization.Serializable

/** 내 문의 목록 — 최신 순 최대 50건. 없으면 404 가 아니라 빈 배열이다. */
@Serializable
data class InquiryListResponse(
    val inquiries: List<InquirySummaryResponse> = emptyList(),
)
