package com.soma369.laimory.core.domain.usecase.user

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.user.AccountWithdrawalOutcome
import com.soma369.laimory.core.domain.model.user.UserProfile
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountWithdrawalUseCaseTest {
    // --- RequestAccountWithdrawalUseCase ---

    @Test
    fun `접수되면 Accepted로 수렴한다`() =
        runTest {
            val useCase = RequestAccountWithdrawalUseCase(FakeUserRepository())

            assertEquals(AccountWithdrawalOutcome.Accepted, useCase().getOrNull())
        }

    @Test
    fun `401은 실패가 아니라 SessionUnavailable로 수렴한다`() =
        runTest {
            // 호출부가 ApiException.rawCode 를 직접 해석하지 않게 도메인 표현으로 좁힌다.
            val useCase = RequestAccountWithdrawalUseCase(FakeUserRepository(error = unauthorized()))

            val result = useCase()

            assertTrue(result.isSuccess)
            assertEquals(AccountWithdrawalOutcome.SessionUnavailable, result.getOrNull())
        }

    @Test
    fun `403은 401과 달리 그대로 실패로 넘긴다`() =
        runTest {
            // fromCode 가 401·403 을 같은 예외 타입으로 만들지만 정책은 rawCode 401 만 수렴시킨다.
            val error = ApiException.fromCode(403, null, null)
            val useCase = RequestAccountWithdrawalUseCase(FakeUserRepository(error = error))

            assertTrue(useCase().isFailure)
        }

    @Test
    fun `5xx와 네트워크 실패는 공통 정책으로 넘긴다`() =
        runTest {
            listOf(ApiException.ServerException(rawCode = 500), ApiException.NetworkException()).forEach { error ->
                val useCase = RequestAccountWithdrawalUseCase(FakeUserRepository(error = error))

                val result = useCase()

                assertTrue(result.isFailure)
                assertEquals(error, result.exceptionOrNull())
            }
        }

    // --- WithdrawAccountUseCase ---

    @Test
    fun `접수되면 이 기기의 세션을 정리한다`() =
        runTest {
            val auth = FakeAuthRepository()
            val useCase = WithdrawAccountUseCase(RequestAccountWithdrawalUseCase(FakeUserRepository()), auth)

            val result = useCase()

            assertEquals(AccountWithdrawalOutcome.Accepted, result.getOrNull())
            assertTrue(auth.sessionCleared)
            // 서버가 이미 credential 을 무효화했으므로 실패가 예정된 logout 요청은 보내지 않는다.
            assertFalse(auth.loggedOut)
        }

    @Test
    fun `401로 끝나도 죽은 세션을 남기지 않는다`() =
        runTest {
            val auth = FakeAuthRepository()
            val useCase =
                WithdrawAccountUseCase(
                    RequestAccountWithdrawalUseCase(FakeUserRepository(error = unauthorized())),
                    auth,
                )

            val result = useCase()

            assertEquals(AccountWithdrawalOutcome.SessionUnavailable, result.getOrNull())
            assertTrue(auth.sessionCleared)
        }

    @Test
    fun `요청이 실패하면 세션을 정리하지 않는다`() =
        runTest {
            val auth = FakeAuthRepository()
            val useCase =
                WithdrawAccountUseCase(
                    RequestAccountWithdrawalUseCase(FakeUserRepository(error = ApiException.ServerException(rawCode = 500))),
                    auth,
                )

            assertTrue(useCase().isFailure)
            assertFalse(auth.sessionCleared)
        }

    @Test
    fun `세션 정리가 실패하면 탈퇴도 실패로 올린다`() =
        runTest {
            // 죽은 세션이 남았는데 탈퇴 완료로 보이면 안 된다.
            val auth = FakeAuthRepository(clearError = IllegalStateException("store"))
            val useCase = WithdrawAccountUseCase(RequestAccountWithdrawalUseCase(FakeUserRepository()), auth)

            assertTrue(useCase().isFailure)
        }

    private fun unauthorized() = ApiException.fromCode(401, null, -2001)

    private class FakeUserRepository(
        private val error: Throwable? = null,
    ) : UserRepository {
        override suspend fun getMyProfile(): UserProfile = UserProfile.of(null)

        override suspend fun requestAccountWithdrawal() {
            error?.let { throw it }
        }
    }

    private class FakeAuthRepository(
        private val clearError: Throwable? = null,
    ) : AuthRepository {
        var sessionCleared = false
            private set
        var loggedOut = false
            private set

        override fun observeSessionState(): Flow<AuthSessionState> = emptyFlow()

        override fun observeSignedInAccount(): Flow<SignedInAccount?> = emptyFlow()

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ) = Unit

        override suspend fun logout() {
            loggedOut = true
        }

        override suspend fun clearSession() {
            clearError?.let { throw it }
            sessionCleared = true
        }
    }
}
