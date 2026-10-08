package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.network.api.CreditApi
import com.soma369.laimory.core.data.network.api.CreditCostApi
import com.soma369.laimory.core.domain.exception.ApiException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class CreditRemoteDataSourceImplTest {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var server: MockWebServer
    private lateinit var remote: CreditRemoteDataSource

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val converter = json.asConverterFactory("application/json".toMediaType())
        val creditApi =
            Retrofit.Builder()
                .baseUrl(server.url("/a/api/v1/"))
                .addConverterFactory(converter)
                .build()
                .create(CreditApi::class.java)
        val costApi =
            Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .addConverterFactory(converter)
                .build()
                .create(CreditCostApi::class.java)
        remote = CreditRemoteDataSourceImpl(creditApi, costApi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `잔액은 인증 경로에서 받는다`() =
        runTest {
            server.enqueue(success("""{"remainingCredits":42}"""))

            assertEquals(42, remote.getCredit().remainingCredits)
            assertEquals("/a/api/v1/credit", server.takeRequest().path)
        }

    @Test
    fun `비용은 공개 경로에서 받고 새로 붙는 필드는 넘긴다`() =
        runTest {
            // 서버는 크레딧을 쓰는 기능이 늘면 같은 응답에 필드를 더한다고 예고했다.
            server.enqueue(success("""{"timelineCreation":1,"somethingNew":3}"""))

            assertEquals(1, remote.getCosts().timelineCreation)
            assertEquals("/api/v1/credit/costs", server.takeRequest().path)
        }

    @Test
    fun `크레딧 행이 없는 500 은 0 이 아니라 실패다`() =
        runTest {
            // 0 으로 바꾸면 남은 게 없다는 뜻이 되어 만들 수 있는 사용자를 막는다.
            server.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"header":{"code":-500,"message":"error"},"body":null}"""),
            )

            try {
                remote.getCredit()
                fail("500 은 예외여야 한다")
            } catch (e: ApiException) {
                assertEquals(500, e.rawCode)
            }
        }

    private fun success(body: String) =
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"header":{"code":0,"message":""},"body":$body}""")
}
