package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.user.toDomain
import com.soma369.laimory.core.data.network.api.UserApi
import com.soma369.laimory.core.domain.exception.ApiException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class UserRemoteDataSourceImplTest {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var server: MockWebServer
    private lateinit var remote: UserRemoteDataSource

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val api =
            Retrofit.Builder()
                .baseUrl(server.url("/a/api/v1/"))
                .addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType()),
                ).build()
                .create(UserApi::class.java)
        remote = UserRemoteDataSourceImpl(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `인증 경로의 users me를 GET으로 조회한다`() =
        runTest {
            server.enqueue(success("""{"nickname":"김소마"}"""))

            val response = remote.getMyProfile()

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/a/api/v1/users/me", request.path)
            assertEquals("김소마", response.nickname)
        }

    @Test
    fun `명시적 null 닉네임은 오류가 아니라 값 없음으로 옮긴다`() =
        runTest {
            // 서버는 key 를 생략하지 않고 JSON null 을 보낸다. 닉네임 미설정은 정상 상태다.
            server.enqueue(success("""{"nickname":null}"""))

            assertNull(remote.getMyProfile().toDomain().nickname)
        }

    @Test
    fun `공백뿐인 닉네임도 값 없음으로 옮긴다`() =
        runTest {
            server.enqueue(success("""{"nickname":"   "}"""))

            assertNull(remote.getMyProfile().toDomain().nickname)
        }

    @Test
    fun `401은 인증 예외로 정규화해 세션 만료 정책이 받게 한다`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"header":{"code":-2001,"message":"unauthorized"}}"""),
            )

            val error =
                try {
                    remote.getMyProfile()
                    null
                } catch (e: ApiException) {
                    e
                }

            assertEquals(401, error?.rawCode)
        }

    // --- 회원 탈퇴 ---

    @Test
    fun `인증 경로의 users me를 DELETE로 요청하고 request body를 보내지 않는다`() =
        runTest {
            server.enqueue(accepted())

            remote.requestAccountWithdrawal()

            val request = server.takeRequest()
            assertEquals("DELETE", request.method)
            assertEquals("/a/api/v1/users/me", request.path)
            assertEquals(0L, request.bodySize)
        }

    @Test
    fun `202와 body가 null인 공통 envelope를 성공으로 받는다`() =
        runTest {
            // HTTP 무바디가 아니라 envelope 의 body 만 null 이다. safeApiCall 은 무바디를 다루지 못한다.
            server.enqueue(accepted())

            remote.requestAccountWithdrawal()
        }

    @Test
    fun `탈퇴 요청의 401도 인증 예외로 정규화한다`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"header":{"code":-2001,"message":"unauthorized"}}"""),
            )

            val error =
                try {
                    remote.requestAccountWithdrawal()
                    null
                } catch (e: ApiException) {
                    e
                }

            assertEquals(401, error?.rawCode)
            assertEquals(-2001, error?.errorCode)
        }

    @Test
    fun `탈퇴 요청의 5xx는 서버 예외로 정규화한다`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500))

            val error =
                try {
                    remote.requestAccountWithdrawal()
                    null
                } catch (e: ApiException) {
                    e
                }

            assertEquals(500, error?.rawCode)
        }

    private fun accepted() =
        MockResponse()
            .setResponseCode(202)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"header":{"code":0,"message":""},"body":null}""")

    private fun success(body: String) =
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"header":{"code":0,"message":null},"body":$body}""")
}
