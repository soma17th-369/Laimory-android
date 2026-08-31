package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.terms.TermAgreementHistoryResponse
import com.soma369.laimory.core.data.model.terms.TermListResponse
import com.soma369.laimory.core.data.model.terms.request.TermAgreementCreateRequest

interface TermsRemoteDataSource {
    suspend fun getCurrentTerms(termTypes: List<String>): TermListResponse

    /** 게시된 정본에서 같은 조회를 한 번 더 한다. 열람 링크 전용 임시 경로다. */
    suspend fun getPublishedTerms(
        url: String,
        termTypes: List<String>,
    ): TermListResponse

    suspend fun getMyAgreements(): TermAgreementHistoryResponse

    suspend fun agreeToTerms(request: TermAgreementCreateRequest)
}
