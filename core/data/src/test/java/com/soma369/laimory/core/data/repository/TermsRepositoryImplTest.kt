package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.TermsRemoteDataSource
import com.soma369.laimory.core.data.model.terms.TermAgreementHistoryResponse
import com.soma369.laimory.core.data.model.terms.TermAgreementResponse
import com.soma369.laimory.core.data.model.terms.TermListResponse
import com.soma369.laimory.core.data.model.terms.TermResponse
import com.soma369.laimory.core.data.model.terms.request.TermAgreementCreateRequest
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.StaleTermVersionException
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class TermsRepositoryImplTest {
    @Test
    fun `앱이 모르는 종류는 버리고 나머지를 옮긴다`() =
        runTest {
            // 서버 catalog 에 앱보다 먼저 새 종류가 생길 수 있다. 하나 때문에 응답 전체를 잃으면
            // 아는 약관까지 못 보여 준다.
            val remote =
                FakeTermsRemoteDataSource(
                    terms =
                        listOf(
                            termResponse("TERMS_OF_SERVICE"),
                            termResponse("MARKETING_CONSENT"),
                        ),
                )

            val documents = TermsRepositoryImpl(remote).getCurrentTerms(listOf(TermType.TERMS_OF_SERVICE))

            assertEquals(listOf(TermType.TERMS_OF_SERVICE), documents.map { it.termType })
        }

    @Test
    fun `해석할 수 없는 시각은 버린다`() =
        runTest {
            val remote = FakeTermsRemoteDataSource(terms = listOf(termResponse("TERMS_OF_SERVICE", effectiveAt = "언젠가")))

            assertTrue(TermsRepositoryImpl(remote).getCurrentTerms(listOf(TermType.TERMS_OF_SERVICE)).isEmpty())
        }

    @Test
    fun `중복 종류는 보내기 전에 거른다`() =
        runTest {
            // 서버는 중복을 400 으로 돌려준다.
            val remote = FakeTermsRemoteDataSource()

            TermsRepositoryImpl(remote).getCurrentTerms(
                listOf(TermType.TERMS_OF_SERVICE, TermType.TERMS_OF_SERVICE),
            )

            assertEquals(listOf("TERMS_OF_SERVICE"), remote.requestedTypes)
        }

    @Test
    fun `개정 경쟁은 재확인이 필요한 실패로 세운다`() =
        runTest {
            // 공통 예외로 흘리면 호출부가 일반 오류처럼 재시도하게 되고, 그 재시도가 사용자가
            // 읽지 않은 버전을 등록한다.
            val remote =
                FakeTermsRemoteDataSource(
                    agreeFailure = ApiException.ConflictException(errorCode = -3002, rawCode = 409),
                )

            val error = runCatching { TermsRepositoryImpl(remote).agree(listOf(document())) }.exceptionOrNull()

            assertTrue(error is StaleTermVersionException)
        }

    @Test
    fun `그 밖의 실패는 그대로 올린다`() =
        runTest {
            val remote =
                FakeTermsRemoteDataSource(
                    agreeFailure = ApiException.ClientException(errorCode = -400, rawCode = 400),
                )

            val error = runCatching { TermsRepositoryImpl(remote).agree(listOf(document())) }.exceptionOrNull()

            assertTrue(error is ApiException.ClientException)
        }

    private fun termResponse(
        termType: String,
        effectiveAt: String = "2026-08-28T00:00:00",
    ) = TermResponse(
        termType = termType,
        version = "1.0",
        title = "제목",
        contentUrl = "https://laimory.app/terms/slug/1.0",
        effectiveAt = effectiveAt,
    )

    private fun document() =
        TermDocument(
            termType = TermType.TERMS_OF_SERVICE,
            version = "1.0",
            title = "제목",
            contentUrl = "https://laimory.app/terms/terms-of-service/1.0",
            effectiveAt = LocalDateTime.of(2026, 8, 28, 0, 0),
        )

    private class FakeTermsRemoteDataSource(
        private val terms: List<TermResponse> = emptyList(),
        private val agreements: List<TermAgreementResponse> = emptyList(),
        private val agreeFailure: Throwable? = null,
    ) : TermsRemoteDataSource {
        var requestedTypes: List<String> = emptyList()

        override suspend fun getCurrentTerms(termTypes: List<String>): TermListResponse {
            requestedTypes = termTypes
            return TermListResponse(terms)
        }

        override suspend fun getMyAgreements() = TermAgreementHistoryResponse(agreements)

        override suspend fun agreeToTerms(request: TermAgreementCreateRequest) {
            agreeFailure?.let { throw it }
        }
    }
}
