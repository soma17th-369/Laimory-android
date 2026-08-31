package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.terms.TermAgreementHistoryResponse
import com.soma369.laimory.core.data.model.terms.TermListResponse
import com.soma369.laimory.core.data.model.terms.request.TermAgreementCreateRequest

interface TermsRemoteDataSource {
    suspend fun getCurrentTerms(termTypes: List<String>): TermListResponse

    suspend fun getMyAgreements(): TermAgreementHistoryResponse

    suspend fun agreeToTerms(request: TermAgreementCreateRequest)
}
