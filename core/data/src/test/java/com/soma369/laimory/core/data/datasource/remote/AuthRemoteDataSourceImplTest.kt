package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.network.api.AuthApi
import com.soma369.laimory.core.domain.exception.ApiException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AuthRemoteDataSourceImplTest {
    private lateinit var server: MockWebServer
    private lateinit var remote: AuthRemoteDataSource

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true }
        val api =
            Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(AuthApi::class.java)
        remote = AuthRemoteDataSourceImpl(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `appCode와 appVerifier로 token을 발급한다`() =
        runTest {
            server.enqueue(successTokens("access-1", "refresh-1"))

            val response = remote.issueTokens("one-time-code", "verifier")
            val request = server.takeRequest()

            assertEquals("/api/v1/auth/token", request.path)
            assertEquals("POST", request.method)
            assertEquals(
                Json.parseToJsonElement("""{"appCode":"one-time-code","appVerifier":"verifier"}"""),
                Json.parseToJsonElement(request.body.readUtf8()),
            )
            assertEquals("access-1", response.accessToken)
            assertEquals("refresh-1", response.refreshToken)
            assertFalse(response.toString().contains("access-1"))
        }

    @Test
    fun `refresh 요청은 회전된 token 쌍을 반환한다`() =
        runTest {
            server.enqueue(successTokens("access-2", "refresh-2"))

            val response = remote.refreshTokens("refresh-1")
            val request = server.takeRequest()

            assertEquals("/api/v1/auth/refresh", request.path)
            assertEquals(
                Json.parseToJsonElement("""{"refreshToken":"refresh-1"}"""),
                Json.parseToJsonElement(request.body.readUtf8()),
            )
            assertEquals("access-2", response.accessToken)
            assertEquals("refresh-2", response.refreshToken)
        }

    @Test
    fun `logout 성공 body가 null이어도 완료한다`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"header":{"code":"COMMON_0000","message":"success"},"body":null}"""),
            )

            remote.logout("refresh-1")

            val request = server.takeRequest()
            assertEquals("/api/v1/auth/logout", request.path)
            assertEquals(Json.parseToJsonElement("""{"refreshToken":"refresh-1"}"""), Json.parseToJsonElement(request.body.readUtf8()))
        }

    @Test
    fun `refresh 거절의 ERROR_2003을 보존한다`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"header":{"code":"ERROR_2003","message":"refresh rejected"},"body":null}"""),
            )

            val error = runCatching { remote.refreshTokens("invalid") }.exceptionOrNull()

            assertTrue(error is ApiException.UnauthorizedException)
            assertEquals("ERROR_2003", (error as ApiException).errorCode)
        }

    private fun successTokens(
        accessToken: String,
        refreshToken: String,
    ): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "header": {"code": "COMMON_0000", "message": "success"},
                  "body": {"accessToken": "$accessToken", "refreshToken": "$refreshToken"}
                }
                """.trimIndent(),
            )
}
