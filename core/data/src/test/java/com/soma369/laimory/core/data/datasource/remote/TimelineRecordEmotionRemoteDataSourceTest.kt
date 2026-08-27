package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.network.api.TimelineRecordApi
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.LocalDate

/** 감정 교체(`PUT .../emotion`) 계약 고정. */
class TimelineRecordEmotionRemoteDataSourceTest {
    private val recordDate = LocalDate.of(2026, 5, 8)

    private lateinit var server: MockWebServer
    private lateinit var remote: TimelineRecordRemoteDataSource

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val api =
            Retrofit.Builder()
                .baseUrl(server.url("/a/api/v1/"))
                .addConverterFactory(
                    Json.asConverterFactory("application/json".toMediaType()),
                ).build()
                .create(TimelineRecordApi::class.java)
        remote = TimelineRecordRemoteDataSourceImpl(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `감정 교체는 날짜 경로에 PUT 으로 literal 을 보낸다`() =
        runTest {
            server.enqueue(success())

            remote.updateDailyRecordEmotion(recordDate, TimelineEmotion.HAPPY)

            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/a/api/v1/timeline/daily-records/2026-05-08/emotion", request.path)
            assertEquals(
                Json.parseToJsonElement("""{"emotionType":"HAPPY"}"""),
                Json.parseToJsonElement(request.body.readUtf8()),
            )
        }

    @Test
    fun `같은 값 재요청도 성공으로 받는다`() =
        runTest {
            // 서버가 멱등이라 재시도가 안전하다. 클라이언트가 중복을 걸러 낼 이유가 없다.
            server.enqueue(success())
            server.enqueue(success())

            remote.updateDailyRecordEmotion(recordDate, TimelineEmotion.NEUTRAL)
            remote.updateDailyRecordEmotion(recordDate, TimelineEmotion.NEUTRAL)

            assertEquals(2, server.requestCount)
        }

    @Test
    fun `DRAFT 기록은 409 -1020 으로 온다`() =
        runTest {
            server.enqueue(failure(409, -1020, "DAILY_RECORD_NOT_SAVED"))

            val exception =
                assertThrows(ApiException::class.java) {
                    kotlinx.coroutines.runBlocking {
                        remote.updateDailyRecordEmotion(recordDate, TimelineEmotion.HAPPY)
                    }
                }

            assertEquals(-1020, exception.errorCode)
        }

    @Test
    fun `없는 기록은 404 -404 로 온다`() =
        runTest {
            server.enqueue(failure(404, -404, "NOT_FOUND"))

            val exception =
                assertThrows(ApiException::class.java) {
                    kotlinx.coroutines.runBlocking {
                        remote.updateDailyRecordEmotion(recordDate, TimelineEmotion.HAPPY)
                    }
                }

            assertEquals(-404, exception.errorCode)
        }

    @Test
    fun `표시 전용 감정은 요청에 실리지 않는다`() =
        runTest {
            // UNKNOWN 은 조회에서 모르는 literal 을 수렴시키는 값이라 보내면 서버가 400 으로 거절한다.
            assertThrows(IllegalArgumentException::class.java) {
                kotlinx.coroutines.runBlocking {
                    remote.updateDailyRecordEmotion(recordDate, TimelineEmotion.UNKNOWN)
                }
            }
            assertEquals(0, server.requestCount)
        }

    private fun success() =
        MockResponse()
            .setResponseCode(200)
            .setBody("""{"header":{"code":0,"message":"success"},"body":null}""")

    private fun failure(
        httpCode: Int,
        errorCode: Int,
        message: String,
    ) = MockResponse()
        .setResponseCode(httpCode)
        .setBody("""{"header":{"code":$errorCode,"message":"$message"},"body":null}""")
}
