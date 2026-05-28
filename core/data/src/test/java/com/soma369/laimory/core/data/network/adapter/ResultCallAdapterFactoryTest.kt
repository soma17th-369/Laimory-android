package com.soma369.laimory.core.data.network.adapter

import com.soma369.laimory.core.domain.exception.ApiException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

class ResultCallAdapterFactoryTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: TestApi

    @Serializable
    data class TestDto(val id: Int, val name: String)

    interface TestApi {
        @GET("/test")
        suspend fun getTest(): Result<TestDto>
    }

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val json = Json { ignoreUnknownKeys = true }
        api =
            Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))
                .client(OkHttpClient())
                .addCallAdapterFactory(ResultCallAdapterFactory(json))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TestApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `HTTP 200 success=true이면 Result_success(data) 반환`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"success":true,"message":"ok","data":{"id":1,"name":"test"},"error":null}"""),
            )

            val result = api.getTest()

            assertTrue(result.isSuccess)
            assertEquals(TestDto(id = 1, name = "test"), result.getOrNull())
        }

    @Test
    fun `HTTP 200 success=false이면 Result_failure(UnknownException) 반환`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"success":false,"message":"비즈니스 오류","data":null,"error":null}"""),
            )

            val result = api.getTest()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is ApiException.UnknownException)
            assertEquals("비즈니스 오류", result.exceptionOrNull()?.message)
        }

    @Test
    fun `HTTP 401이면 Result_failure(UnauthorizedException) 반환`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"message":"인증이 필요합니다"}"""),
            )

            val result = api.getTest()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is ApiException.UnauthorizedException)
        }

    @Test
    fun `HTTP 404이면 Result_failure(ClientException) 반환`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .setBody("""{"message":"리소스를 찾을 수 없습니다"}"""),
            )

            val result = api.getTest()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is ApiException.ClientException)
        }

    @Test
    fun `HTTP 500이면 Result_failure(ServerException) 반환`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("""{"message":"서버 오류"}"""),
            )

            val result = api.getTest()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is ApiException.ServerException)
        }

    @Test
    fun `HTTP 409이면 Result_failure(ConflictException) 반환`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(409)
                    .setBody("""{"message":"중복된 요청"}"""),
            )

            val result = api.getTest()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is ApiException.ConflictException)
        }

    @Test
    fun `네트워크 오류이면 Result_failure(NetworkException) 반환`() =
        runTest {
            mockWebServer.shutdown()

            val result = api.getTest()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is ApiException.NetworkException)
        }

    @Test
    fun `HTTP 200 success=true data=null이면 Result_failure(UnknownException) 반환`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"success":true,"message":"ok","data":null,"error":null}"""),
            )

            val result = api.getTest()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is ApiException.UnknownException)
        }
}
