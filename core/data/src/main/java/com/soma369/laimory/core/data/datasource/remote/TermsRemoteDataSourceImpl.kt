package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.terms.TermAgreementHistoryResponse
import com.soma369.laimory.core.data.model.terms.TermListResponse
import com.soma369.laimory.core.data.model.terms.request.TermAgreementCreateRequest
import com.soma369.laimory.core.data.network.api.TermAgreementApi
import com.soma369.laimory.core.data.network.api.TermsApi
import com.soma369.laimory.core.data.network.safeApiCall
import com.soma369.laimory.core.data.network.safeApiCallUnit
import javax.inject.Inject

class TermsRemoteDataSourceImpl
    @Inject
    constructor(
        private val termsApi: TermsApi,
        private val agreementApi: TermAgreementApi,
    ) : TermsRemoteDataSource {
        override suspend fun getCurrentTerms(termTypes: List<String>): TermListResponse =
            safeApiCall { termsApi.getCurrentTerms(termTypes) }

        override suspend fun getPublishedTerms(
            url: String,
            termTypes: List<String>,
        ): TermListResponse = safeApiCall { termsApi.getPublishedTerms(url, termTypes) }

        override suspend fun getMyAgreements(): TermAgreementHistoryResponse = safeApiCall { agreementApi.getMyAgreements() }

        // 등록 성공은 공통 envelope 의 body 가 비어 있다.
        override suspend fun agreeToTerms(request: TermAgreementCreateRequest) = safeApiCallUnit { agreementApi.agreeToTerms(request) }
    }
