package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.BuildConfig
import com.soma369.laimory.core.data.datasource.remote.TermsRemoteDataSource
import com.soma369.laimory.core.data.di.PublishedTermsBaseUrl
import com.soma369.laimory.core.data.model.terms.request.toRequest
import com.soma369.laimory.core.data.model.terms.toDomain
import com.soma369.laimory.core.data.network.ApiPrefix
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.StaleTermVersionException
import com.soma369.laimory.core.domain.model.terms.TermAgreement
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.repository.TermsRepository
import javax.inject.Inject

private const val TERMS_PATH = "terms"

class TermsRepositoryImpl
    @Inject
    constructor(
        private val remoteDataSource: TermsRemoteDataSource,
        @PublishedTermsBaseUrl private val publishedBaseUrl: String = "",
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

        /**
         * 이 환경에 문서가 있으면 그대로 쓰고, 비어 있을 때만 게시된 정본에서 한 번 더 가져온다.
         *
         * 대체 위치가 비어 있으면(운영 빌드) 대체 자체가 돌지 않는다. 개발 catalog 에 seed 가
         * 들어가면 이 경로는 더 이상 타지 않으므로 그때 지운다.
         */
        override suspend fun getPublishedTerms(types: List<TermType>): List<TermDocument> {
            val current = getCurrentTerms(types)
            if (current.isNotEmpty()) return current

            if (publishedBaseUrl.isBlank()) return current
            val url = ApiPrefix.publicBaseUrl(publishedBaseUrl, BuildConfig.API_APP_VERSION) + TERMS_PATH
            return remoteDataSource
                .getPublishedTerms(url, types.distinct().map { it.name })
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
