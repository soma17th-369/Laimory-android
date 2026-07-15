package com.soma369.laimory.core.domain.usecase.auth

import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private class FakeAuthRepository : AuthRepository {
        var appCode: String? = null
        var appVerifier: String? = null

        override fun observeSessionState(): Flow<AuthSessionState> = emptyFlow()

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ) {
            this.appCode = appCode
            this.appVerifier = appVerifier
        }

        override suspend fun logout() = Unit
    }
}
