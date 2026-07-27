package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.push.PushRegistrationRequest
import com.soma369.laimory.core.data.network.api.PushRegistrationApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class PushRegistrationRemoteDataSourceImplTest {
    private lateinit var server: MockWebServer
    private lateinit var remote: PushRegistrationRemoteDataSource

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
                .create(PushRegistrationApi::class.java)
        remote = PushRegistrationRemoteDataSourceImpl(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `FID 등록은 원문을 PUT body로 보내고 null body 성공을 허용한다`() =
        runTest {
            server.enqueue(success())

            remote.register("opaque-fid")

            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/a/api/v1/push-registrations", request.path)
            assertEquals(
                Json.parseToJsonElement("""{"firebaseInstallationId":"opaque-fid"}"""),
                Json.parseToJsonElement(request.body.readUtf8()),
            )
        }

    @Test
    fun `FID 해제는 DELETE body를 사용한다`() =
        runTest {
            server.enqueue(success())

            remote.unregister("opaque-fid")

            val request = server.takeRequest()
            assertEquals("DELETE", request.method)
            assertEquals(
                Json.parseToJsonElement("""{"firebaseInstallationId":"opaque-fid"}"""),
                Json.parseToJsonElement(request.body.readUtf8()),
            )
        }

    @Test
    fun `request 문자열 표현에 FID를 노출하지 않는다`() {
        assertFalse(PushRegistrationRequest("opaque-fid").toString().contains("opaque-fid"))
    }

    private fun success() =
        MockResponse()
            .setResponseCode(200)
            .setBody("""{"header":{"code":0,"message":"success"},"body":null}""")
}
