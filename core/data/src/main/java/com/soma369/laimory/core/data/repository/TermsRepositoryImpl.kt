package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.TermsRemoteDataSource
import com.soma369.laimory.core.data.model.terms.request.toRequest
import com.soma369.laimory.core.data.model.terms.toDomain
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.StaleTermVersionException
import com.soma369.laimory.core.domain.model.terms.TermAgreement
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.repository.TermsRepository
import javax.inject.Inject

class TermsRepositoryImpl
    @Inject
    constructor(
        private val remoteDataSource: TermsRemoteDataSource,
    ) : TermsRepository {
        override suspend fun getCurrentTerms(types: List<TermType>): List<TermDocument> {
            if (types.isEmpty()) return emptyList()
            // 중복 종류는 400 이라 보내기 전에 한 번 거른다.
            val requested = types.distinct()
            return remoteDataSource
                .getCurrentTerms(requested.map { it.name })
                .terms
                .mapNotNull { it.toDomain() }
        }

        override suspend fun getMyAgreements(): List<TermAgreement> =
            remoteDataSource.getMyAgreements().agreements.mapNotNull { it.toDomain() }

        override suspend fun agree(documents: List<TermDocument>) {
            try {
                remoteDataSource.agreeToTerms(documents.toRequest())
            } catch (exception: ApiException) {
                // 개정 경쟁은 재시도 대상이 아니라 사용자에게 다시 물어야 하는 상황이라 따로 세운다.
                if (exception.errorCode == StaleTermVersionException.ERROR_CODE) {
                    throw StaleTermVersionException(exception)
                }
                throw exception
            }
        }
    }
