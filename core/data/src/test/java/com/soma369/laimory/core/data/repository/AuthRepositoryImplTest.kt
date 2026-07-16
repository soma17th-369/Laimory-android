package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.auth.PendingLoginProviderStore
import com.soma369.laimory.core.data.datasource.remote.AuthRemoteDataSource
import com.soma369.laimory.core.data.model.auth.response.TokenResponse
import com.soma369.laimory.core.data.session.AuthSessionOperationLock
import com.soma369.laimory.core.data.session.TokenSession
import com.soma369.laimory.core.data.session.TokenSessionStore
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {
    private val remote = FakeAuthRemoteDataSource()
    private val store = FakeTokenSessionStore()
    private val providerStore = FakePendingLoginProviderStore()
    private val repository = AuthRepositoryImpl(remote, store, providerStore, AuthSessionOperationLock())

    @Test
    fun `세션 관찰은 Loading 후 저장 상태를 방출한다`() =
        runTest {
            val states = repository.observeSessionState().take(2).toList()

            assertEquals(
                listOf(AuthSessionState.Loading, AuthSessionState.Unauthenticated),
                states,
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `token 교체는 동일한 인증 상태를 중복 방출하지 않는다`() =
        runTest {
            val states = mutableListOf<AuthSessionState>()
            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    repository.observeSessionState().toList(states)
                }

            store.save(TokenSession("first-access", "first-refresh"))
            runCurrent()
            store.save(TokenSession("second-access", "second-refresh"))
            runCurrent()

            assertEquals(
                listOf(
                    AuthSessionState.Loading,
                    AuthSessionState.Unauthenticated,
                    AuthSessionState.Authenticated,
                ),
                states,
            )
            collectJob.cancelAndJoin()
        }

    @Test
    fun `token 발급 성공 시 access refresh 쌍을 함께 저장한다`() =
        runTest {
            remote.issueResponse = TokenResponse("new-access", "new-refresh")
            providerStore.provider = SocialLoginProvider.GOOGLE

            repository.issueTokens("code", "verifier")

            assertEquals("new-access", store.session?.accessToken)
            assertEquals("new-refresh", store.session?.refreshToken)
            assertEquals(SocialLoginProvider.GOOGLE.name, store.session?.loginProvider)
            assertNull(providerStore.provider)
        }

    @Test
    fun `저장된 세션의 로그인 제공자를 계정 정보로 관찰한다`() =
        runTest {
            store.save(TokenSession("access", "refresh", loginProvider = SocialLoginProvider.KAKAO.name))

            val account = repository.observeSignedInAccount().take(1).toList().single()

            assertEquals(SocialLoginProvider.KAKAO, account?.provider)
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

    @Test
    fun `logout이 취소되어도 로컬 세션을 제거하고 취소를 전파한다`() =
        runTest {
            store.save(TokenSession("access", "refresh"))
            remote.suspendLogout = true
            val logoutJob = launch { repository.logout() }
            remote.logoutStarted.await()

            logoutJob.cancelAndJoin()

            assertTrue(logoutJob.isCancelled)
            assertNull(store.session)
            assertTrue(store.clearCount > 0)
        }

    private class FakeAuthRemoteDataSource : AuthRemoteDataSource {
        var issueResponse = TokenResponse("access", "refresh")
        var logoutError: Exception? = null
        var logoutToken: String? = null
        var suspendLogout = false
        val logoutStarted = CompletableDeferred<Unit>()

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ): TokenResponse = issueResponse

        override suspend fun refreshTokens(refreshToken: String): TokenResponse = error("Not used")

        override suspend fun logout(refreshToken: String) {
            logoutToken = refreshToken
            if (suspendLogout) {
                logoutStarted.complete(Unit)
                awaitCancellation()
            }
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

    private class FakePendingLoginProviderStore : PendingLoginProviderStore {
        var provider: SocialLoginProvider? = null

        override suspend fun save(provider: SocialLoginProvider) {
            this.provider = provider
        }

        override suspend fun get(): SocialLoginProvider? = provider

        override suspend fun clear() {
            provider = null
        }
    }
}
