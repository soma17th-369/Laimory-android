package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.model.user.UserProfile
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.UserRepository
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import com.soma369.laimory.core.domain.usecase.user.GetUserProfileUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultUserProfileCoordinatorTest {
    private val google = SignedInAccount(provider = SocialLoginProvider.GOOGLE)

    @Test
    fun `로그인 세션이 있으면 회원 정보를 조회해 공유한다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeUserRepository(UserProfile.of("김소마"))
            val accounts = MutableStateFlow<SignedInAccount?>(google)
            val coordinator = coordinator(repository, accounts)

            runCurrent()

            assertEquals("김소마", coordinator.profile.value?.nickname)
            assertEquals(1, repository.callCount)
        }

    @Test
    fun `닉네임이 없는 계정도 조회 성공으로 남아 다시 묻지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // `아직 모른다`와 `없는 게 확실하다`가 구분돼야 재조회가 반복되지 않는다.
            val repository = FakeUserRepository(UserProfile.of(null))
            val coordinator = coordinator(repository, MutableStateFlow(google))
            runCurrent()

            coordinator.refresh()
            runCurrent()

            assertNull(coordinator.profile.value?.nickname)
            assertTrue(coordinator.profile.value != null)
            assertEquals(1, repository.callCount)
        }

    @Test
    fun `여러 화면이 동시에 요구해도 요청은 하나다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeUserRepository(UserProfile.of("김소마"), isSuspended = true)
            val coordinator = coordinator(repository, MutableStateFlow(google))
            runCurrent()

            // 홈과 설정이 각각 refresh 를 부르는 상황.
            coordinator.refresh()
            coordinator.refresh()
            runCurrent()

            assertEquals(1, repository.callCount)

            repository.complete()
            runCurrent()
            assertEquals("김소마", coordinator.profile.value?.nickname)
        }

    @Test
    fun `성공한 세션에서는 다시 조회하지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeUserRepository(UserProfile.of("김소마"))
            val coordinator = coordinator(repository, MutableStateFlow(google))
            runCurrent()

            coordinator.refresh()
            runCurrent()

            assertEquals(1, repository.callCount)
        }

    @Test
    fun `로그아웃하면 닉네임을 즉시 비운다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeUserRepository(UserProfile.of("김소마"))
            val accounts = MutableStateFlow<SignedInAccount?>(google)
            val coordinator = coordinator(repository, accounts)
            runCurrent()

            accounts.value = null
            runCurrent()

            // 설정 화면에 이전 계정 닉네임이 남으면 안 된다.
            assertNull(coordinator.profile.value)
        }

    @Test
    fun `같은 제공자로 재로그인해도 새로 조회한다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeUserRepository(UserProfile.of("김소마"))
            val accounts = MutableStateFlow<SignedInAccount?>(google)
            val coordinator = coordinator(repository, accounts)
            runCurrent()

            accounts.value = null
            runCurrent()
            accounts.value = google
            runCurrent()

            // Root 교체는 ViewModel 재생성을 보장하지 않으므로 세션 전이가 유일한 갱신 근거다.
            assertEquals(2, repository.callCount)
            assertEquals("김소마", coordinator.profile.value?.nickname)
        }

    @Test
    fun `조회 도중 로그아웃하면 늦게 온 응답을 버린다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeUserRepository(UserProfile.of("김소마"), isSuspended = true)
            val accounts = MutableStateFlow<SignedInAccount?>(google)
            val coordinator = coordinator(repository, accounts)
            runCurrent()

            accounts.value = null
            runCurrent()
            repository.complete()
            runCurrent()

            // 계정을 바꿨는데 이전 계정 닉네임이 뜨면 안 된다.
            assertNull(coordinator.profile.value)
        }

    @Test
    fun `실패하면 비워 두고 refresh 로 다시 시도할 수 있다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeUserRepository(UserProfile.of("김소마"), failuresBeforeSuccess = 1)
            val coordinator = coordinator(repository, MutableStateFlow(google))
            runCurrent()

            assertNull(coordinator.profile.value)

            coordinator.refresh()
            runCurrent()

            assertEquals("김소마", coordinator.profile.value?.nickname)
            assertEquals(2, repository.callCount)
        }

    @Test
    fun `401 만 세션 만료로 알리고 그 밖의 실패는 조용히 지나간다`() =
        runTest(UnconfinedTestDispatcher()) {
            val messageHelper = RecordingMessageHelper()

            val serverError =
                FakeUserRepository(UserProfile.of("김소마"), failuresBeforeSuccess = 1, failureCode = 500)
            coordinator(serverError, MutableStateFlow(google), messageHelper)
            runCurrent()
            // 사용자가 요청한 적 없는 배경 조회라 홈 진입마다 오류 안내가 뜨면 안 된다.
            assertTrue(messageHelper.sent.isEmpty())

            val unauthorized =
                FakeUserRepository(UserProfile.of("김소마"), failuresBeforeSuccess = 1, failureCode = 401)
            coordinator(unauthorized, MutableStateFlow(google), messageHelper)
            runCurrent()
            assertEquals(listOf(UserMessage.SessionExpired), messageHelper.sent)
        }

    private fun TestScope.coordinator(
        repository: FakeUserRepository,
        accounts: MutableStateFlow<SignedInAccount?>,
        messageHelper: MessageHelper = RecordingMessageHelper(),
    ): DefaultUserProfileCoordinator =
        DefaultUserProfileCoordinator(
            getUserProfileUseCase = GetUserProfileUseCase(repository, messageHelper),
            observeSignedInAccountUseCase = ObserveSignedInAccountUseCase(FakeAuthRepository(accounts)),
            applicationScope = backgroundScope,
        )

    private class FakeUserRepository(
        private val profile: UserProfile,
        private val isSuspended: Boolean = false,
        private var failuresBeforeSuccess: Int = 0,
        private val failureCode: Int = 500,
    ) : UserRepository {
        var callCount = 0
            private set

        private var gate = CompletableDeferred<Unit>()

        fun complete() {
            gate.complete(Unit)
        }

        override suspend fun getMyProfile(): UserProfile {
            callCount++
            if (isSuspended) gate.await()
            if (failuresBeforeSuccess > 0) {
                failuresBeforeSuccess--
                throw ApiException.fromCode(failureCode)
            }
            return profile
        }

        /** 이 대역은 회원 정보 조회만 검증한다. */
        override suspend fun requestAccountWithdrawal() = Unit
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

    private class RecordingMessageHelper : MessageHelper {
        val sent = mutableListOf<UserMessage>()

        override fun send(message: UserMessage) {
            sent += message
        }
    }
}
