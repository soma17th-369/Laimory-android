package com.soma369.laimory.core.domain.usecase.auth

import com.soma369.laimory.core.domain.exception.SocialLoginException
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.auth.SocialLoginAttempt
import com.soma369.laimory.core.domain.model.auth.SocialLoginCallback
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.SocialLoginRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompleteSocialLoginUseCaseTest {
    private val socialRepository = FakeSocialLoginRepository()
    private val authRepository = FakeAuthRepository()
    private val useCase = CompleteSocialLoginUseCase(socialRepository, IssueAuthTokensUseCase(authRepository))

    @Test
    fun `callback code와 보관된 verifier를 한 번 교환한다`() =
        runBlocking {
            socialRepository.pendingVerifier = "verifier"

            val first = useCase(SocialLoginCallback(appCode = "app-code"))
            val duplicate = useCase(SocialLoginCallback(appCode = "app-code"))

            assertTrue(first.isSuccess)
            assertTrue(duplicate.exceptionOrNull() is SocialLoginException.MissingAttempt)
            assertEquals("app-code", authRepository.appCode)
            assertEquals("verifier", authRepository.appVerifier)
            assertEquals(1, authRepository.issueCount)
        }

    @Test
    fun `provider 오류 callback은 pending 시도를 폐기한다`() =
        runBlocking {
            socialRepository.pendingVerifier = "verifier"

            val result = useCase(SocialLoginCallback(errorCode = "-2004"))

            assertTrue(result.exceptionOrNull() is SocialLoginException.ProviderFailure)
            assertEquals(1, socialRepository.clearCount)
            assertEquals(0, authRepository.issueCount)
        }

    @Test
    fun `code와 error가 없는 callback은 pending 시도를 폐기한다`() =
        runBlocking {
            socialRepository.pendingVerifier = "verifier"

            val result = useCase(SocialLoginCallback())

            assertTrue(result.exceptionOrNull() is SocialLoginException.InvalidCallback)
            assertEquals(1, socialRepository.clearCount)
        }

    private class FakeSocialLoginRepository : SocialLoginRepository {
        var pendingVerifier: String? = null
        var clearCount = 0

        override suspend fun start(provider: SocialLoginProvider): SocialLoginAttempt = error("Not used")

        override suspend fun consumePendingVerifier(): String? = pendingVerifier.also { pendingVerifier = null }

        override suspend fun clearPendingAttempt() {
            clearCount++
            pendingVerifier = null
        }
    }

    private class FakeAuthRepository : AuthRepository {
        var appCode: String? = null
        var appVerifier: String? = null
        var issueCount = 0

        override fun observeSessionState(): Flow<AuthSessionState> = emptyFlow()

        override fun observeSignedInAccount(): Flow<SignedInAccount?> = emptyFlow()

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ) {
            issueCount++
            this.appCode = appCode
            this.appVerifier = appVerifier
        }

        override suspend fun logout() = Unit

        override suspend fun clearSession() = Unit
    }
}
