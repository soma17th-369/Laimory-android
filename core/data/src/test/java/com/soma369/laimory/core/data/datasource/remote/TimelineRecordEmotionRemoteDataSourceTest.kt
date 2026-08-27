package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.network.api.TimelineRecordApi
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.timeline.CreateTimelineEventCommand
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime

/** 감정 교체(`PUT .../emotion`)와 Event 생성(`POST .../events`) 계약 고정. */
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

    @Test
    fun `Event 생성은 다섯 키를 모두 보내고 값이 없는 키도 null 로 남긴다`() =
        runTest {
            // 서버는 키 누락을 400 으로 거절한다. 전역 Json 이 explicitNulls=false 라 그냥 두면
            // subtitle·endAt 이 통째로 빠진다.
            server.enqueue(createdEvent())

            remote.createTimelineEvent(
                CreateTimelineEventCommand(
                    recordDate = recordDate,
                    eventType = TimelineEventType.MEAL,
                    title = "점심",
                    subtitle = null,
                    startAt = LocalDateTime.of(2026, 5, 8, 12, 30),
                    endAt = null,
                    memo = null,
                ),
            )

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/a/api/v1/timeline/daily-records/2026-05-08/events", request.path)
            assertEquals(
                Json.parseToJsonElement(
                    """{"eventType":"MEAL","title":"점심","subtitle":null,"startAt":"2026-05-08T12:30:00","endAt":null}""",
                ),
                Json.parseToJsonElement(request.body.readUtf8()),
            )
        }

    @Test
    fun `공백 메모는 키째 보내지 않는다`() =
        runTest {
            // 누락·null·blank 가 모두 메모 없음이라 굳이 빈 문자열을 실어 보낼 이유가 없다.
            server.enqueue(createdEvent())

            remote.createTimelineEvent(
                CreateTimelineEventCommand(
                    recordDate = recordDate,
                    eventType = TimelineEventType.REST,
                    title = "산책",
                    subtitle = "한강",
                    startAt = LocalDateTime.of(2026, 5, 8, 18, 0),
                    endAt = LocalDateTime.of(2026, 5, 8, 19, 0),
                    memo = "   ",
                ),
            )

            val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
            assertFalse(body.containsKey("memo"))
            assertFalse(body.containsKey("photosToAdd"))
        }

    private fun createdEvent() =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """{"header":{"code":0,"message":"success"},"body":{"timelineEventId":1,"eventType":"REST",
                |"startAt":"2026-05-08T12:30:00","endAt":null,"title":"점심","subtitle":null,"memo":null,
                |"question":null,"items":[]}}
                """.trimMargin().replace("\n", ""),
            )

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
