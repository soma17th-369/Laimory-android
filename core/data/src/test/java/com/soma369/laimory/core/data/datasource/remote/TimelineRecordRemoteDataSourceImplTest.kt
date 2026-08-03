package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.network.api.TimelineRecordApi
import com.soma369.laimory.core.domain.exception.ApiException
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
    fun `DailyRecord 삭제는 recordDate 경로에 DELETE하고 null body 성공을 처리한다`() =
        runTest {
            server.enqueue(successUnitResponse())

            remote.deleteDailyRecord(RECORD_DATE)

            val request = server.takeRequest()
            assertEquals("DELETE", request.method)
            assertEquals("/timeline/daily-records/2026-07-27", request.path)
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
