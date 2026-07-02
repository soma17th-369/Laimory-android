package com.soma369.laimory.core.data.network

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.domain.exception.ApiException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

class SafeApiCallTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: TestApi

    @Serializable
    data class TestDto(
        val id: Int,
        val name: String,
    )

    interface TestApi {
        @GET("/test")
        suspend fun getTest(): Response<ApiResponse<TestDto>>
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                coerceInputValues = true
            }
        api =
            Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TestApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `header code가 성공이면 body 반환`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"header":{"code":"COMMON_0000","message":"success"},"body":{"id":1,"name":"test"}}"""),
            )

            val result = safeApiCall { api.getTest() }

            assertEquals(TestDto(id = 1, name = "test"), result)
        }

    @Test
    fun `header code가 성공이 아니면 UnknownException + errorCode 보존`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"header":{"code":"AUTH_0001","message":"권한이 없습니다"},"body":null}"""),
            )

            val error = runCatching { safeApiCall { api.getTest() } }.exceptionOrNull()

            assertTrue(error is ApiException.UnknownException)
            assertEquals("권한이 없습니다", error?.message)
            assertEquals("AUTH_0001", (error as ApiException).errorCode)
        }

    @Test
    fun `HTTP 401이면 UnauthorizedException`() =
        runTest {
            mockWebServer.enqueue(MockResponse().setResponseCode(401))
            assertTrue(runCatching { safeApiCall { api.getTest() } }.exceptionOrNull() is ApiException.UnauthorizedException)
        }

    @Test
    fun `HTTP 404이면 ClientException`() =
        runTest {
            mockWebServer.enqueue(MockResponse().setResponseCode(404))
            assertTrue(runCatching { safeApiCall { api.getTest() } }.exceptionOrNull() is ApiException.ClientException)
        }

    @Test
    fun `HTTP 500이면 ServerException`() =
        runTest {
            mockWebServer.enqueue(MockResponse().setResponseCode(500))
            assertTrue(runCatching { safeApiCall { api.getTest() } }.exceptionOrNull() is ApiException.ServerException)
        }

    @Test
    fun `HTTP 409이면 ConflictException`() =
        runTest {
            mockWebServer.enqueue(MockResponse().setResponseCode(409))
            assertTrue(runCatching { safeApiCall { api.getTest() } }.exceptionOrNull() is ApiException.ConflictException)
        }

    @Test
    fun `네트워크 오류면 NetworkException`() =
        runTest {
            mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
            assertTrue(runCatching { safeApiCall { api.getTest() } }.exceptionOrNull() is ApiException.NetworkException)
        }

    @Test
    fun `역직렬화 불가면 UnknownException`() =
        runTest {
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("not json"))
            assertTrue(runCatching { safeApiCall { api.getTest() } }.exceptionOrNull() is ApiException.UnknownException)
        }

    @Test
    fun `HTTP 실패 시 errorBody의 message를 예외 메시지로 전달`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(400).setBody("""{"message":"잘못된 파라미터"}"""),
            )

            val error = runCatching { safeApiCall { api.getTest() } }.exceptionOrNull()

            assertTrue(error is ApiException.ClientException)
            assertEquals("잘못된 파라미터", error?.message)
        }

    @Test
    fun `errorBody에 message가 없으면 error 필드를 사용`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(500).setBody("""{"status":500,"error":"서버 내부 오류"}"""),
            )

            val error = runCatching { safeApiCall { api.getTest() } }.exceptionOrNull()

            assertTrue(error is ApiException.ServerException)
            assertEquals("서버 내부 오류", error?.message)
        }
}
