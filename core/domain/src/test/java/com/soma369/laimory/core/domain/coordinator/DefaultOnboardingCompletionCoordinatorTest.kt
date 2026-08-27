package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.OnboardingRepository
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultOnboardingCompletionCoordinatorTest {
    private val google = SignedInAccount(provider = SocialLoginProvider.GOOGLE)

    @Test
    fun `로그인 세션이 있으면 서버 값을 조회해 공유한다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeOnboardingRepository(remoteCompletion = Result.success(true))
            val coordinator = coordinator(repository, MutableStateFlow(google))

            runCurrent()

            assertEquals(true, coordinator.completed.value)
            assertEquals(1, repository.fetchCount)
        }

    @Test
    fun `조회 전에는 모름을 유지한다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 여기서 값을 단정하면 앱 루트가 반대쪽 화면을 한 프레임 띄운 뒤 갈린다.
            val repository = FakeOnboardingRepository(remoteCompletion = Result.success(true))
            val coordinator = coordinator(repository, MutableStateFlow(null))

            runCurrent()

            assertNull(coordinator.completed.value)
        }

    @Test
    fun `조회에 실패하면 캐시로 떨어진다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository =
                FakeOnboardingRepository(
                    remoteCompletion = Result.failure(IllegalStateException("500")),
                    cached = true,
                )
            val coordinator = coordinator(repository, MutableStateFlow(google))

            runCurrent()

            assertEquals(true, coordinator.completed.value)
        }

    @Test
    fun `조회도 캐시도 없으면 아직 안 한 것으로 본다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 앱을 로딩에 묶어 두면 오프라인 사용자가 들어오지 못한다. 최악이 온보딩을 한 번 더
            // 보는 것이라면 그쪽이 낫다 — 완료 기록은 멱등이다.
            val repository =
                FakeOnboardingRepository(remoteCompletion = Result.failure(IllegalStateException("offline")))
            val coordinator = coordinator(repository, MutableStateFlow(google))

            runCurrent()

            assertEquals(false, coordinator.completed.value)
        }

    @Test
    fun `로그아웃하면 모름으로 되돌리고 캐시를 비운다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 비우지 않으면 이전 계정의 완료가 다음 계정으로 샌다.
            val repository = FakeOnboardingRepository(remoteCompletion = Result.success(true))
            val accounts = MutableStateFlow<SignedInAccount?>(google)
            val coordinator = coordinator(repository, accounts)
            runCurrent()

            accounts.value = null
            runCurrent()

            assertNull(coordinator.completed.value)
            assertTrue(repository.cleared)
        }

    @Test
    fun `완료는 서버 응답을 기다리지 않고 값을 올린다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeOnboardingRepository(remoteCompletion = Result.success(false))
            val coordinator = coordinator(repository, MutableStateFlow(google))
            runCurrent()

            coordinator.markCompleted()

            assertEquals(true, coordinator.completed.value)
            assertEquals(1, repository.recordCount)
            assertEquals(true, repository.cached)
        }

    @Test
    fun `초기화는 다시 조회하지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 다시 조회하면 서버가 곧장 완료라고 답해 온보딩이 열리자마자 닫힌다.
            val repository = FakeOnboardingRepository(remoteCompletion = Result.success(true))
            val coordinator = coordinator(repository, MutableStateFlow(google))
            runCurrent()
            val fetchesBeforeReset = repository.fetchCount

            coordinator.resetForCurrentSession()
            runCurrent()

            assertFalse(coordinator.completed.value!!)
            assertEquals(fetchesBeforeReset, repository.fetchCount)
        }

    private fun TestScope.coordinator(
        repository: OnboardingRepository,
        accounts: MutableStateFlow<SignedInAccount?>,
    ) = DefaultOnboardingCompletionCoordinator(
        repository = repository,
        observeSignedInAccountUseCase = ObserveSignedInAccountUseCase(FakeAuthRepository(accounts)),
        // backgroundScope 는 테스트가 끝날 때 취소된다. this 로 주면 계정 관찰이 끝나지 않아 멈춘다.
        applicationScope = backgroundScope,
    )

    private class FakeOnboardingRepository(
        private val remoteCompletion: Result<Boolean>,
        var cached: Boolean? = null,
    ) : OnboardingRepository {
        var fetchCount = 0
        var recordCount = 0
        var cleared = false

        override suspend fun cachedCompletion(): Boolean? = cached

        override suspend fun cacheCompletion(isCompleted: Boolean) {
            cached = isCompleted
        }

        override suspend fun recordCompletion() {
            recordCount++
        }

        override suspend fun fetchCompletion(): Result<Boolean> {
            fetchCount++
            return remoteCompletion
        }

        override fun observeLastPageKey(): Flow<String?> = flowOf(null)

        override suspend fun saveProgress(pageKey: String) = Unit

        override suspend fun clear() {
            cleared = true
            cached = null
        }
    }

    private class FakeAuthRepository(
        private val accounts: Flow<SignedInAccount?>,
    ) : AuthRepository {
        override fun observeSignedInAccount(): Flow<SignedInAccount?> = accounts

        override fun observeSessionState(): Flow<AuthSessionState> = emptyFlow()

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ) = Unit

        override suspend fun logout() = Unit

        override suspend fun clearSession() = Unit
    }
}
