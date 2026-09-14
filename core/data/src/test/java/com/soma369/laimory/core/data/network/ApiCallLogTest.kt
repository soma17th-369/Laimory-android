package com.soma369.laimory.core.data.network

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.network.interceptor.ApiCallLogInterceptor
import com.soma369.laimory.core.util.logging.CrashReporter
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/** API 로그가 크래시 리포트로 나갈 수 있는 문장만 남기는지 본다. */
class ApiCallLogTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var api: TestApi
    private val reporter = RecordingCrashReporter()

    @Serializable
    data class TestDto(
        val id: Int,
    )

    interface TestApi {
        @GET("timeline/events/{timelineEventId}")
        suspend fun getEvent(
            @Path("timelineEventId") timelineEventId: Long,
        ): Response<ApiResponse<TestDto>>
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun setUp() {
        Logger.crashReporter = reporter
        Logger.remoteMinLevel = Logger.Level.INFO
        server = MockWebServer().apply { start() }
        client = OkHttpClient.Builder().addInterceptor(ApiCallLogInterceptor()).build()
        val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                coerceInputValues = true
            }
        api =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TestApi::class.java)
    }

    @After
    fun tearDown() {
        Logger.crashReporter = null
        server.shutdown()
    }

    @Test
    fun `경로는 식별자를 뺀 선언 템플릿으로 남긴다`() =
        runTest {
            server.enqueue(MockResponse().setBody("""{"header":{"code":0,"message":""},"body":{"id":1}}"""))

            safeApiCall { api.getEvent(4242) }

            assertEquals(listOf("INFO/Network: GET timeline/events/{timelineEventId} → 200"), reporter.messages)
        }

    @Test
    fun `성공이 아닌 HTTP 코드는 WARN 으로 남긴다`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500))

            runCatching { safeApiCall { api.getEvent(1) } }

            assertEquals(listOf("WARN/Network: GET timeline/events/{timelineEventId} → 500"), reporter.messages)
        }

    @Test
    fun `통신이 끊겨도 경로를 남긴다`() =
        runTest {
            // 이때 safeApiCall 은 예외만 받아 요청을 볼 수 없다. 인터셉터에 두는 이유다.
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

            runCatching { safeApiCall { api.getEvent(1) } }

            val line = reporter.messages.single()
            assertTrue(line, line.startsWith("WARN/Network: GET timeline/events/{timelineEventId} → 통신 실패("))
        }

    @Test
    fun `헤더 코드 실패는 코드만 남기고 서버 메시지는 넣지 않는다`() =
        runTest {
            server.enqueue(
                MockResponse().setBody("""{"header":{"code":-2001,"message":"권한이 없습니다"},"body":null}"""),
            )

            runCatching { safeApiCall { api.getEvent(1) } }

            assertEquals(
                listOf(
                    "INFO/Network: GET timeline/events/{timelineEventId} → 200",
                    "WARN/Network: GET timeline/events/{timelineEventId} → 헤더 코드 -2001",
                ),
                reporter.messages,
            )
        }

    @Test
    fun `응답을 해석하지 못하면 원문 없이 예외 종류만 남긴다`() =
        runTest {
            val secret = "secret-access-token"
            server.enqueue(MockResponse().setBody("""{"header":{"code":0,"message":""},"body":{"id":"$secret"}}"""))

            runCatching { safeApiCall { api.getEvent(1) } }

            assertTrue(reporter.messages.any { it.startsWith("WARN/Network: 응답 처리 실패(") })
            assertFalse(reporter.messages.any { it.contains(secret) })
        }

    @Test
    fun `Retrofit 이 만들지 않은 요청은 경로를 적지 않는다`() {
        server.enqueue(MockResponse())

        client.newCall(Request.Builder().url(server.url("/timeline/daily-records/2026-09-14")).build()).execute().close()

        assertEquals(listOf("INFO/Network: GET (경로 미상) → 200"), reporter.messages)
    }

    private class RecordingCrashReporter : CrashReporter {
        val messages = mutableListOf<String>()

        override fun log(message: String) {
            messages += message
        }

        override fun recordException(throwable: Throwable) = Unit

        override fun setKey(
            key: String,
            value: String,
        ) = Unit
    }
}
