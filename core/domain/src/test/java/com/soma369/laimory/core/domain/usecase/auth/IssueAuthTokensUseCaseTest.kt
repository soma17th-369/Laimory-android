package com.soma369.laimory.core.domain.usecase.auth

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueAuthTokensUseCaseTest {
    @Test
    fun `일회용 code와 verifier를 repository에 전달한다`() =
        runBlocking {
            val repository = FakeAuthRepository()
            val useCase = IssueAuthTokensUseCase(repository)

            val result = useCase(IssueAuthTokensParams("code", "verifier"))

            assertTrue(result.isSuccess)
            assertEquals("code", repository.appCode)
            assertEquals("verifier", repository.appVerifier)
        }

    @Test
    fun `민감한 입력은 문자열 표현에 노출하지 않는다`() {
        val params = IssueAuthTokensParams("secret-code", "secret-verifier")

        assertFalse(params.toString().contains("secret-code"))
        assertFalse(params.toString().contains("secret-verifier"))
    }

    @Test
    fun `API 실패는 로그인 화면이 처리할 Result로 반환한다`() =
        runBlocking {
            val repository = FakeAuthRepository()
            val error = ApiException.UnauthorizedException(errorCode = "ERROR_2002", rawCode = 401)
            repository.issueError = error

            val result = IssueAuthTokensUseCase(repository)(IssueAuthTokensParams("code", "verifier"))

            assertSame(error, result.exceptionOrNull())
        }

    @Test
    fun `프로그래밍 오류는 로그인 실패로 변환하지 않는다`() {
        val repository = FakeAuthRepository()
        repository.issueError = IllegalStateException("unexpected")

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                IssueAuthTokensUseCase(repository)(IssueAuthTokensParams("code", "verifier"))
            }
        }
    }

    private class FakeAuthRepository : AuthRepository {
        var appCode: String? = null
        var appVerifier: String? = null
        var issueError: Throwable? = null

        override fun observeSessionState(): Flow<AuthSessionState> = emptyFlow()

        override fun observeSignedInAccount(): Flow<SignedInAccount?> = emptyFlow()

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ) {
            issueError?.let { throw it }
            this.appCode = appCode
            this.appVerifier = appVerifier
        }

        override suspend fun logout() = Unit
    }
}
