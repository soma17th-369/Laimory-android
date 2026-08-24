package com.soma369.laimory.core.domain.usecase.user

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
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
            val useCase = requestUseCase(FakeUserRepository())

            assertEquals(AccountWithdrawalOutcome.Accepted, useCase().getOrNull())
        }

    @Test
    fun `401은 실패가 아니라 SessionUnavailable로 수렴하고 세션 만료를 알리지 않는다`() =
        runTest {
            // 탈퇴 직후의 401 은 만료가 아니라 계정이 사라진 결과라 "세션이 만료되었습니다"가 틀린 문장이다.
            val messageHelper = RecordingMessageHelper()
            val useCase = requestUseCase(FakeUserRepository(error = unauthorized()), messageHelper)

            val result = useCase()

            assertTrue(result.isSuccess)
            assertEquals(AccountWithdrawalOutcome.SessionUnavailable, result.getOrNull())
            assertTrue(messageHelper.sent.isEmpty())
        }

    @Test
    fun `403은 401과 달리 그대로 실패로 넘긴다`() =
        runTest {
            // fromCode 가 401·403 을 같은 예외 타입으로 만들지만 정책은 rawCode 401 만 수렴시킨다.
            val messageHelper = RecordingMessageHelper()
            val useCase = requestUseCase(FakeUserRepository(error = ApiException.fromCode(403, null, null)), messageHelper)

            val result = useCase()

            assertTrue(result.isFailure)
            // 403 은 공통 정책이 다루지 않는 코드라 화면이 처리하도록 그대로 내려간다.
            assertTrue(messageHelper.sent.isEmpty())
            assertFalse(result.exceptionOrNull() is HandledException)
        }

    @Test
    fun `5xx와 404는 공통 정책 메시지를 발행한다`() =
        runTest {
            val cases =
                listOf(
                    ApiException.ServerException(rawCode = 500) to UserMessage.TemporaryUnavailable,
                    ApiException.fromCode(404, null, null) to UserMessage.UnsupportedFeature,
                )

            cases.forEach { (error, expected) ->
                val messageHelper = RecordingMessageHelper()
                val useCase = requestUseCase(FakeUserRepository(error = error), messageHelper)

                val result = useCase()

                assertTrue(result.isFailure)
                assertEquals(listOf(expected), messageHelper.sent)
                // 공통 정책이 이미 알렸으므로 ViewModel 이 재알림하지 않도록 감싸 내린다.
                assertTrue(result.exceptionOrNull() is HandledException)
            }
        }

    @Test
    fun `네트워크 실패는 공통 메시지 없이 화면으로 내려간다`() =
        runTest {
            val messageHelper = RecordingMessageHelper()
            val error = ApiException.NetworkException()
            val useCase = requestUseCase(FakeUserRepository(error = error), messageHelper)

            val result = useCase()

            assertEquals(error, result.exceptionOrNull())
            assertTrue(messageHelper.sent.isEmpty())
        }

    // --- WithdrawAccountUseCase ---

    @Test
    fun `접수되면 이 기기의 세션을 정리한다`() =
        runTest {
            val auth = FakeAuthRepository()
            val useCase = WithdrawAccountUseCase(requestUseCase(FakeUserRepository()), auth)

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
                WithdrawAccountUseCase(requestUseCase(FakeUserRepository(error = unauthorized())), auth)

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
                    requestUseCase(FakeUserRepository(error = ApiException.ServerException(rawCode = 500))),
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
            val useCase = WithdrawAccountUseCase(requestUseCase(FakeUserRepository()), auth)

            assertTrue(useCase().isFailure)
        }

    private fun unauthorized() = ApiException.fromCode(401, null, -2001)

    private fun requestUseCase(
        repository: FakeUserRepository,
        messageHelper: MessageHelper = RecordingMessageHelper(),
    ) = RequestAccountWithdrawalUseCase(repository, messageHelper)

    private class RecordingMessageHelper : MessageHelper {
        val sent = mutableListOf<UserMessage>()

        override fun send(message: UserMessage) {
            sent += message
        }
    }

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
