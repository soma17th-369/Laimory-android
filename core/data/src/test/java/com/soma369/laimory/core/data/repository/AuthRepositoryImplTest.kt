package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.AuthRemoteDataSource
import com.soma369.laimory.core.data.model.auth.response.TokenResponse
import com.soma369.laimory.core.data.session.AuthSessionOperationLock
import com.soma369.laimory.core.data.session.TokenSession
import com.soma369.laimory.core.data.session.TokenSessionStore
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {
    private val remote = FakeAuthRemoteDataSource()
    private val store = FakeTokenSessionStore()
    private val repository = AuthRepositoryImpl(remote, store, AuthSessionOperationLock())

    @Test
    fun `세션 관찰은 Loading 후 저장 상태를 방출한다`() =
        runTest {
            val states = repository.observeSessionState().take(2).toList()

            assertEquals(
                listOf(AuthSessionState.Loading, AuthSessionState.Unauthenticated),
                states,
            )
        }

    @Test
    fun `token 발급 성공 시 access refresh 쌍을 함께 저장한다`() =
        runTest {
            remote.issueResponse = TokenResponse("new-access", "new-refresh")

            repository.issueTokens("code", "verifier")

            assertEquals("new-access", store.session?.accessToken)
            assertEquals("new-refresh", store.session?.refreshToken)
        }

    @Test
    fun `logout API가 실패해도 로컬 세션을 제거한다`() =
        runTest {
            store.save(TokenSession("access", "refresh"))
            remote.logoutError = IllegalStateException("offline")

            repository.logout()

            assertNull(store.session)
            assertEquals("refresh", remote.logoutToken)
            assertTrue(store.clearCount > 0)
        }

    private class FakeAuthRemoteDataSource : AuthRemoteDataSource {
        var issueResponse = TokenResponse("access", "refresh")
        var logoutError: Exception? = null
        var logoutToken: String? = null

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ): TokenResponse = issueResponse

        override suspend fun refreshTokens(refreshToken: String): TokenResponse = error("Not used")

        override suspend fun logout(refreshToken: String) {
            logoutToken = refreshToken
            logoutError?.let { throw it }
        }
    }

    private class FakeTokenSessionStore : TokenSessionStore {
        private val state = MutableStateFlow<TokenSession?>(null)
        var clearCount = 0
        val session: TokenSession? get() = state.value

        override fun observe(): Flow<TokenSession?> = state

        override suspend fun get(): TokenSession? = state.value

        override suspend fun save(session: TokenSession) {
            state.value = session
        }

        override suspend fun clear() {
            clearCount++
            state.value = null
        }
    }
}
