package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.timeline.request.UpdateTimelineEventMemoRequest
import com.soma369.laimory.core.data.network.api.TimelineRecordApi
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.LocalDate

class TimelineRecordRemoteDataSourceImplTest {
    private lateinit var server: MockWebServer
    private lateinit var remote: TimelineRecordRemoteDataSource

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                coerceInputValues = true
            }
        val api =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TimelineRecordApi::class.java)
        remote = TimelineRecordRemoteDataSourceImpl(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `전체 조회는 daily-records 경로에 GET하고 서버 순서를 보존해 반환한다`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "header":{"code":0,"message":""},
                          "body":{
                            "timelines":[
                              {"dailyRecordId":32,"recordDate":"2026-07-28","events":[]},
                              {"dailyRecordId":31,"recordDate":"2026-07-27","events":[]}
                            ]
                          }
                        }
                        """.trimIndent(),
                    ),
            )

            val response = remote.getDailyRecords()

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/timeline/daily-records", request.path)
            assertEquals(listOf(32L, 31L), response.timelines.map { it.dailyRecordId })
        }

    @Test
    fun `단건 조회는 recordDate 경로에 GET하고 graph 응답을 반환한다`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "header":{"code":0,"message":""},
                          "body":{
                            "dailyRecordId":31,
                            "recordDate":"2026-07-27",
                            "emotionType":"HAPPY",
                            "events":[
                              {
                                "timelineEventId":41,
                                "eventType":"MEAL",
                                "startAt":"2026-07-27T12:00:00",
                                "endAt":null,
                                "title":"점심",
                                "subtitle":null,
                                "memo":null,
                                "items":[]
                              }
                            ]
                          }
                        }
                        """.trimIndent(),
                    ),
            )

            val response = remote.getDailyRecord(RECORD_DATE)

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/timeline/daily-records/2026-07-27", request.path)
            assertEquals(31L, response.dailyRecordId)
            assertEquals(41L, response.events.single().timelineEventId)
        }

    @Test
    fun `단건 미존재 응답은 404와 -404 오류 코드를 보존한다`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .setBody(
                        """
                        {
                          "header":{"code":-404,"message":"기록을 찾을 수 없습니다"},
                          "body":null
                        }
                        """.trimIndent(),
                    ),
            )

            val failure = runCatching { remote.getDailyRecord(RECORD_DATE) }.exceptionOrNull()

            assertTrue(failure is ApiException.ClientException)
            val apiException = failure as ApiException
            assertEquals(404, apiException.rawCode)
            assertEquals(-404, apiException.errorCode)
        }

    @Test
    fun `Event 수정은 Event 경로에 PATCH하고 null body 성공을 처리한다`() =
        runTest {
            server.enqueue(successUnitResponse())

            remote.updateTimelineEvent(
                timelineEventId = 17L,
                request = buildJsonObject { put("title", "수정 제목") },
            )

            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/timeline/events/17", request.path)
            assertEquals("""{"title":"수정 제목"}""", request.body.readUtf8())
        }

    @Test
    fun `Event 메모 수정은 전용 경로에 PUT하고 memo를 전송한다`() =
        runTest {
            server.enqueue(successUnitResponse())

            remote.updateTimelineEventMemo(
                timelineEventId = 17L,
                request = UpdateTimelineEventMemoRequest("오늘의 메모"),
            )

            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/timeline/events/17/memo", request.path)
            assertEquals("""{"memo":"오늘의 메모"}""", request.body.readUtf8())
        }

    @Test
    fun `Event 메모 제거는 memo 필드를 생략한 PUT body로 전송한다`() =
        runTest {
            server.enqueue(successUnitResponse())

            remote.updateTimelineEventMemo(
                timelineEventId = 17L,
                request = UpdateTimelineEventMemoRequest(null),
            )

            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/timeline/events/17/memo", request.path)
            assertEquals("{}", request.body.readUtf8())
        }

    @Test
    fun `Event 단건 조회는 Event ID 경로에 GET하고 갱신 응답을 반환한다`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "header":{"code":0,"message":""},
                          "body":{
                            "timelineEventId":17,
                            "eventType":"WORK",
                            "startAt":"2026-07-08T14:00:00",
                            "endAt":null,
                            "title":"수정 제목",
                            "subtitle":null,
                            "memo":null,
                            "items":[]
                          }
                        }
                        """.trimIndent(),
                    ),
            )

            val response = remote.getTimelineEvent(17L)

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/timeline/events/17", request.path)
            assertEquals(17L, response.timelineEventId)
        }

    @Test
    fun `Event 삭제는 Event ID 경로에 DELETE하고 null body 성공을 처리한다`() =
        runTest {
            server.enqueue(successUnitResponse())

            remote.deleteTimelineEvent(17L)

            val request = server.takeRequest()
            assertEquals("DELETE", request.method)
            assertEquals("/timeline/events/17", request.path)
        }

    @Test
    fun `Event 사진 삭제는 Event와 Item ID 경로에 DELETE하고 null body 성공을 처리한다`() =
        runTest {
            server.enqueue(successUnitResponse())

            remote.deleteTimelineEventPhoto(timelineEventId = 17L, timelineItemId = 31L)

            val request = server.takeRequest()
            assertEquals("DELETE", request.method)
            assertEquals("/timeline/events/17/items/31", request.path)
        }

    @Test
    fun `Event 사진 타입 불일치 응답은 400과 -1018 오류 코드를 보존한다`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(400)
                    .setBody(
                        """
                        {
                          "header":{"code":-1018,"message":"PHOTO Item만 삭제할 수 있습니다"},
                          "body":null
                        }
                        """.trimIndent(),
                    ),
            )

            val failure =
                runCatching {
                    remote.deleteTimelineEventPhoto(timelineEventId = 17L, timelineItemId = 31L)
                }.exceptionOrNull()

            assertTrue(failure is ApiException.ClientException)
            val apiException = failure as ApiException
            assertEquals(400, apiException.rawCode)
            assertEquals(-1018, apiException.errorCode)
        }

    @Test
    fun `DailyRecord 삭제는 recordDate 경로에 DELETE하고 null body 성공을 처리한다`() =
        runTest {
            server.enqueue(successUnitResponse())

            remote.deleteDailyRecord(RECORD_DATE)

            val request = server.takeRequest()
            assertEquals("DELETE", request.method)
            assertEquals("/timeline/daily-records/2026-07-27", request.path)
        }

    @Test
    fun `하루 기록 저장은 날짜 save 경로에 선택한 감정을 body로 POST한다`() =
        runTest {
            server.enqueue(successUnitResponse())

            remote.saveDailyRecord(RECORD_DATE, TimelineEmotion.HAPPY)

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/timeline/daily-records/2026-07-27/save", request.path)
            assertEquals("""{"emotionType":"HAPPY"}""", request.body.readUtf8())
        }

    @Test
    fun `선택 가능한 감정 5종은 서버 literal 그대로 직렬화된다`() =
        runTest {
            val literals =
                TimelineEmotion.SELECTABLE.map { emotion ->
                    server.enqueue(successUnitResponse())
                    remote.saveDailyRecord(RECORD_DATE, emotion)
                    server.takeRequest().body.readUtf8()
                }

            assertEquals(
                listOf(
                    """{"emotionType":"VERY_HAPPY"}""",
                    """{"emotionType":"HAPPY"}""",
                    """{"emotionType":"NEUTRAL"}""",
                    """{"emotionType":"UNHAPPY"}""",
                    """{"emotionType":"VERY_UNHAPPY"}""",
                ),
                literals,
            )
        }

    @Test
    fun `감정 미상은 저장 요청으로 나가지 못한다`() =
        runTest {
            // UNKNOWN 은 조회에서 모르는 literal 을 수렴시키는 표시 상태다. 서버로 돌려보내면 400 이므로
            // 요청을 만들기 전에 막는다.
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { remote.saveDailyRecord(RECORD_DATE, TimelineEmotion.UNKNOWN) }
            }
        }

    @Test
    fun `사진 삭제 실패 응답은 502와 기능 오류 코드를 보존한다`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(502)
                    .setBody(
                        """
                        {
                          "header":{"code":-1017,"message":"사진 삭제 실패"},
                          "body":null
                        }
                        """.trimIndent(),
                    ),
            )

            val failure = runCatching { remote.deleteTimelineEvent(17L) }.exceptionOrNull()

            assertTrue(failure is ApiException.ServerException)
            val apiException = failure as ApiException
            assertEquals(502, apiException.rawCode)
            assertEquals(-1017, apiException.errorCode)
        }

    private fun successUnitResponse() =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "header":{"code":0,"message":""},
                  "body":null
                }
                """.trimIndent(),
            )

    private companion object {
        val RECORD_DATE: LocalDate = LocalDate.of(2026, 7, 27)
    }
}
