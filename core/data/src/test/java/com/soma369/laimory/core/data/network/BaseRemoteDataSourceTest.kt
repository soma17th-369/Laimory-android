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

class BaseRemoteDataSourceTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var dataSource: TestRemoteDataSource

    @Serializable
    data class TestDto(
        val id: Int,
        val name: String,
    )

    interface TestApi {
        @GET("/test")
        suspend fun getTest(): Response<ApiResponse<TestDto>>
    }

    private class TestRemoteDataSource(
        private val api: TestApi,
        json: Json,
    ) : BaseRemoteDataSource(json) {
        suspend fun getTest(): TestDto = safeApiCall { api.getTest() }
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
        val api =
            Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TestApi::class.java)
        dataSource = TestRemoteDataSource(api, json)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `HTTP 200 success=true이면 data 반환`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"success":true,"message":"ok","data":{"id":1,"name":"test"},"error":null}"""),
            )

            val result = dataSource.getTest()

            assertEquals(TestDto(id = 1, name = "test"), result)
        }

    @Test
    fun `HTTP 200 success=false이면 UnknownException 던짐`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"success":false,"message":"비즈니스 오류","data":null,"error":null}"""),
            )

            val error = runCatching { dataSource.getTest() }.exceptionOrNull()

            assertTrue(error is ApiException.UnknownException)
            assertEquals("비즈니스 오류", error?.message)
        }

    @Test
    fun `HTTP 401이면 UnauthorizedException 던짐`() =
        runTest {
            mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"인증이 필요합니다"}"""))

            val error = runCatching { dataSource.getTest() }.exceptionOrNull()

            assertTrue(error is ApiException.UnauthorizedException)
        }

    @Test
    fun `HTTP 404이면 ClientException 던짐`() =
        runTest {
            mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"찾을 수 없음"}"""))

            val error = runCatching { dataSource.getTest() }.exceptionOrNull()

            assertTrue(error is ApiException.ClientException)
        }

    @Test
    fun `HTTP 500이면 ServerException 던짐`() =
        runTest {
            mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("""{"message":"서버 오류"}"""))

            val error = runCatching { dataSource.getTest() }.exceptionOrNull()

            assertTrue(error is ApiException.ServerException)
        }

    @Test
    fun `HTTP 409이면 ConflictException 던짐`() =
        runTest {
            mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody("""{"message":"중복된 요청"}"""))

            val error = runCatching { dataSource.getTest() }.exceptionOrNull()

            assertTrue(error is ApiException.ConflictException)
        }

    @Test
    fun `네트워크 오류면 NetworkException 던짐`() =
        runTest {
            mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

            val error = runCatching { dataSource.getTest() }.exceptionOrNull()

            assertTrue(error is ApiException.NetworkException)
        }

    @Test
    fun `역직렬화 불가 응답이면 UnknownException 던짐`() =
        runTest {
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("not json"))

            val error = runCatching { dataSource.getTest() }.exceptionOrNull()

            assertTrue(error is ApiException.UnknownException)
        }
}
