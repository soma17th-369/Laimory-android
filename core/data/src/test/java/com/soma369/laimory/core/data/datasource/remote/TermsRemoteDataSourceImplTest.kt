package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.terms.request.TermAgreementCreateRequest
import com.soma369.laimory.core.data.model.terms.request.TermAgreementItem
import com.soma369.laimory.core.data.network.api.TermAgreementApi
import com.soma369.laimory.core.data.network.api.TermsApi
import com.soma369.laimory.core.domain.exception.ApiException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class TermsRemoteDataSourceImplTest {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var server: MockWebServer
    private lateinit var remote: TermsRemoteDataSource

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val converter = json.asConverterFactory("application/json".toMediaType())
        val termsApi =
            Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .addConverterFactory(converter)
                .build()
                .create(TermsApi::class.java)
        val agreementApi =
            Retrofit.Builder()
                .baseUrl(server.url("/a/api/v1/"))
                .addConverterFactory(converter)
                .build()
                .create(TermAgreementApi::class.java)
        remote = TermsRemoteDataSourceImpl(termsApi, agreementApi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `약관 조회는 인증 없는 경로에 종류를 반복 query 로 보낸다`() =
        runTest {
            // 서버는 반복 query 의 순서를 응답 순서로 쓴다. 콤마로 이으면 미지원 값 400 이다.
            server.enqueue(success("""{"terms":[]}"""))

            remote.getCurrentTerms(listOf("TERMS_OF_SERVICE", "PRIVACY_POLICY"))

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals(
                "/api/v1/terms?termTypes=TERMS_OF_SERVICE&termTypes=PRIVACY_POLICY",
                request.path,
            )
        }

    @Test
    fun `활성화 전의 빈 배열은 오류가 아니다`() =
        runTest {
            server.enqueue(success("""{"terms":[]}"""))

            assertTrue(remote.getCurrentTerms(listOf("TERMS_OF_SERVICE")).terms.isEmpty())
        }

    @Test
    fun `동의 등록은 인증 경로에 종류와 버전만 보낸다`() =
        runTest {
            // 수락 시각은 서버가 기록한다. 앱이 보내면 기록의 신뢰를 앱이 쥐게 된다.
            server.enqueue(success("null"))

            remote.agreeToTerms(
                TermAgreementCreateRequest(listOf(TermAgreementItem("TERMS_OF_SERVICE", "1.0"))),
            )

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/a/api/v1/terms/agreements", request.path)
            assertEquals(
                """{"agreements":[{"termType":"TERMS_OF_SERVICE","version":"1.0"}]}""",
                request.body.readUtf8(),
            )
        }

    @Test
    fun `개정 경쟁 409 는 에러 코드를 보존한다`() =
        runTest {
            // 호출부가 이 코드로 재확인 경로를 고른다. 새 버전 자동 등록으로 흘러가면 안 된다.
            server.enqueue(
                MockResponse()
                    .setResponseCode(409)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"header":{"code":-3002,"message":"stale"}}"""),
            )

            val error =
                try {
                    remote.agreeToTerms(TermAgreementCreateRequest(listOf(TermAgreementItem("TERMS_OF_SERVICE", "1.0"))))
                    null
                } catch (e: ApiException) {
                    e
                }

            assertEquals(-3002, error?.errorCode)
        }

    @Test
    fun `이력이 없으면 빈 배열로 온다`() =
        runTest {
            server.enqueue(success("""{"agreements":[]}"""))

            assertTrue(remote.getMyAgreements().agreements.isEmpty())
        }

    private fun success(body: String) =
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"header":{"code":0,"message":""},"body":$body}""")
}
