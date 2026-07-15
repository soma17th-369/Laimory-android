package com.soma369.laimory.core.data.network.interceptor

import com.soma369.laimory.core.data.session.TokenSession
import com.soma369.laimory.core.data.session.TokenSessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthTokenInterceptorTest {
    private lateinit var server: MockWebServer
    private val store = FakeTokenSessionStore()

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `저장된 access token을 Bearer header로 첨부한다`() {
        store.session = TokenSession("access-secret", "refresh-secret")
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder().addInterceptor(AuthTokenInterceptor(store)).build()

        client.newCall(Request.Builder().url(server.url("/protected")).build()).execute().close()

        assertEquals("Bearer access-secret", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `세션이 없으면 Authorization header를 추가하지 않는다`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder().addInterceptor(AuthTokenInterceptor(store)).build()

        client.newCall(Request.Builder().url(server.url("/protected")).build()).execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    private class FakeTokenSessionStore : TokenSessionStore {
        private val state = MutableStateFlow<TokenSession?>(null)
        var session: TokenSession?
            get() = state.value
            set(value) {
                state.value = value
            }

        override fun observe(): Flow<TokenSession?> = state

        override suspend fun get(): TokenSession? = state.value

        override suspend fun save(session: TokenSession) {
            state.value = session
        }

        override suspend fun clear() {
            state.value = null
        }
    }
}
