package com.soma369.laimory.core.data.model.terms

import kotlinx.serialization.Serializable

/** 현재 유효 약관 목록. 활성화 전이면 오류가 아니라 빈 배열이다. */
@Serializable
data class TermListResponse(
    val terms: List<TermResponse> = emptyList(),
)
