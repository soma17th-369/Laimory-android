package com.soma369.laimory.core.data.network.interceptor

import com.soma369.laimory.core.data.datasource.remote.AuthRemoteDataSource
import com.soma369.laimory.core.data.model.auth.response.TokenResponse
import com.soma369.laimory.core.data.session.AuthSessionOperationLock
import com.soma369.laimory.core.data.session.TokenSession
import com.soma369.laimory.core.data.session.TokenSessionStore
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class AuthTokenAuthenticatorTest {
    @Test
    fun `동시 401은 refresh 한 번으로 합치고 모두 회전된 access token을 사용한다`() =
        runTest {
            val loginProvider = SocialLoginProvider.GOOGLE.name
            val store =
                FakeTokenSessionStore(
                    TokenSession("old-access", "old-refresh", loginProvider = loginProvider),
                )
            val remote = FakeAuthRemoteDataSource(TokenResponse("new-access", "new-refresh"), delayMillis = 50)
            val authenticator = AuthTokenAuthenticator(store, remote, AuthSessionOperationLock())
            val response = unauthorizedResponse("old-access", checkNotNull(store.session).sessionId)

            val retried =
                List(5) {
                    async(Dispatchers.Default) { authenticator.authenticate(null, response) }
                }.awaitAll()

            assertEquals(1, remote.refreshCount.get())
            assertTrue(retried.all { it?.header("Authorization") == "Bearer new-access" })
            assertEquals("new-refresh", store.session?.refreshToken)
            assertEquals(loginProvider, store.session?.loginProvider)
        }

    @Test
    fun `refresh가 ERROR_2003으로 거절되면 세션을 제거하고 재시도하지 않는다`() {
        val store = FakeTokenSessionStore(TokenSession("old-access", "old-refresh"))
        val remote =
            FakeAuthRemoteDataSource(
                error =
                    ApiException.UnauthorizedException(
                        message = "rejected",
                        errorCode = "ERROR_2003",
                        rawCode = 401,
                    ),
            )
        val authenticator = AuthTokenAuthenticator(store, remote, AuthSessionOperationLock())

        val retried =
            authenticator.authenticate(
                null,
                unauthorizedResponse("old-access", checkNotNull(store.session).sessionId),
            )

        assertNull(retried)
        assertNull(store.session)
    }

    @Test
    fun `refresh의 일시 서버 실패는 세션을 유지하고 IO 실패로 전달한다`() {
        val store = FakeTokenSessionStore(TokenSession("old-access", "old-refresh"))
        val remote =
            FakeAuthRemoteDataSource(
                error = ApiException.ServerException(message = "temporary", rawCode = 503),
            )
        val authenticator = AuthTokenAuthenticator(store, remote, AuthSessionOperationLock())

        assertThrows(IOException::class.java) {
            authenticator.authenticate(
                null,
                unauthorizedResponse("old-access", checkNotNull(store.session).sessionId),
            )
        }
        assertEquals("old-refresh", store.session?.refreshToken)
    }

    @Test
    fun `과거 세션의 401은 새 세션 token으로 재전송하지 않는다`() {
        val store = FakeTokenSessionStore(TokenSession("new-access", "new-refresh", "new-session"))
        val remote = FakeAuthRemoteDataSource(TokenResponse("rotated", "rotated-refresh"))
        val authenticator = AuthTokenAuthenticator(store, remote, AuthSessionOperationLock())

        val retried = authenticator.authenticate(null, unauthorizedResponse("old-access", "old-session"))

        assertNull(retried)
        assertEquals(0, remote.refreshCount.get())
    }

    @Test
    fun `회전된 access token의 재시도도 401이면 refresh하지 않는다`() {
        val store = FakeTokenSessionStore(TokenSession("new-access", "new-refresh"))
        val remote = FakeAuthRemoteDataSource(TokenResponse("unused-access", "unused-refresh"))
        val authenticator = AuthTokenAuthenticator(store, remote, AuthSessionOperationLock())
        val sessionId = checkNotNull(store.session).sessionId
        val firstResponse = unauthorizedResponse("old-access", sessionId).withoutBody()
        val secondResponse = unauthorizedResponse("new-access", sessionId, firstResponse)

        val retried = authenticator.authenticate(null, secondResponse)

        assertNull(retried)
        assertEquals(0, remote.refreshCount.get())
    }

    private fun unauthorizedResponse(
        accessToken: String,
        sessionId: String,
        priorResponse: Response? = null,
    ): Response {
        val request =
            Request.Builder()
                .url("https://example.test/a/api/v1/protected")
                .header("Authorization", "Bearer $accessToken")
                .tag(AuthSessionRequestTag::class.java, AuthSessionRequestTag(sessionId))
                .build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body(ByteArray(0).toResponseBody())
            .priorResponse(priorResponse)
            .build()
    }

    private fun Response.withoutBody(): Response = newBuilder().body(null).build()

    private class FakeAuthRemoteDataSource(
        private val response: TokenResponse? = null,
        private val error: ApiException? = null,
        private val delayMillis: Long = 0,
    ) : AuthRemoteDataSource {
        val refreshCount = AtomicInteger()

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ): TokenResponse = error("Not used")

        override suspend fun refreshTokens(refreshToken: String): TokenResponse {
            refreshCount.incrementAndGet()
            if (delayMillis > 0) delay(delayMillis)
            error?.let { throw it }
            return checkNotNull(response)
        }

        override suspend fun logout(refreshToken: String) = Unit
    }

    private class FakeTokenSessionStore(initial: TokenSession?) : TokenSessionStore {
        private val state = MutableStateFlow(initial)
        val session: TokenSession? get() = state.value

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
