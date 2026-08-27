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
    fun `로그아웃하면 아직 못 올린 완료도 버린다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 남겨 두면 다음에 들어온 계정이 그 표시를 자기 것으로 읽어, 온보딩을 한 번도 하지
            // 않은 사람이 곧장 홈으로 간다. 계정별로 나눠 보관할 식별자가 앱에 없다.
            val repository =
                FakeOnboardingRepository(remoteCompletion = Result.success(true), pending = true)
            repository.recordFailure = IllegalStateException("offline")
            val accounts = MutableStateFlow<SignedInAccount?>(google)
            coordinator(repository, accounts)
            runCurrent()

            accounts.value = null
            runCurrent()

            assertFalse(repository.pending)
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
    fun `완료 기록이 실패하면 올리지 못한 표시가 남는다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeOnboardingRepository(remoteCompletion = Result.success(false))
            repository.recordFailure = IllegalStateException("offline")
            val coordinator = coordinator(repository, MutableStateFlow(google))
            runCurrent()

            coordinator.markCompleted()

            // 사용자에게는 이미 끝났다고 보였으므로 값은 완료다.
            assertEquals(true, coordinator.completed.value)
            assertTrue(repository.pending)
        }

    @Test
    fun `올리지 못한 완료는 다음 세션에서 다시 올린다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository =
                FakeOnboardingRepository(remoteCompletion = Result.success(false), pending = true)
            val coordinator = coordinator(repository, MutableStateFlow(google))

            runCurrent()

            assertEquals(1, repository.recordCount)
            assertFalse(repository.pending)
        }

    @Test
    fun `아직 못 올린 완료는 서버의 미완료보다 우선한다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 서버는 기록을 못 받았으니 false 를 준다. 끝낸 사람에게 온보딩을 다시 보이면 안 된다.
            val repository =
                FakeOnboardingRepository(remoteCompletion = Result.success(false), pending = true)
            repository.recordFailure = IllegalStateException("offline")
            val coordinator = coordinator(repository, MutableStateFlow(google))

            runCurrent()

            assertEquals(true, coordinator.completed.value)
            assertEquals(0, repository.fetchCount)
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
        var pending: Boolean = false,
    ) : OnboardingRepository {
        var fetchCount = 0
        var recordCount = 0
        var cleared = false
        var recordFailure: Throwable? = null

        override suspend fun cachedCompletion(): Boolean? = cached

        override suspend fun cacheCompletion(isCompleted: Boolean) {
            cached = isCompleted
        }

        override suspend fun recordCompletion() {
            recordCount++
            recordFailure?.let { throw it }
        }

        override suspend fun isCompletionPending(): Boolean = pending

        override suspend fun setCompletionPending(isPending: Boolean) {
            pending = isPending
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
            pending = false
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
