package com.soma369.laimory.core.data.model.terms

import kotlinx.serialization.Serializable

/** 내 동의 이력. 없으면 빈 배열이다. */
@Serializable
data class TermAgreementHistoryResponse(
    val agreements: List<TermAgreementResponse> = emptyList(),
)
