package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.network.api.TimelineRecordApi
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
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

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
    fun `통합 수정은 Event 경로에 PATCH하고 갱신 응답을 반환한다`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "header":{"code":"COMMON_0000","message":"success"},
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

            val response =
                remote.updateTimelineEvent(
                    timelineEventId = 17L,
                    request = buildJsonObject { put("title", "수정 제목") },
                )

            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/timeline/events/17", request.path)
            assertEquals("""{"title":"수정 제목"}""", request.body.readUtf8())
            assertEquals(17L, response.timelineEventId)
        }
}
