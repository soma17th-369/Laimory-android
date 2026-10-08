package com.soma369.laimory.core.domain.usecase.credit

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.credit.CreditCosts
import com.soma369.laimory.core.domain.repository.CreditRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetRemainingCreditsUseCaseTest {
    @Test
    fun `남은 크레딧을 그대로 돌려준다`() =
        runTest {
            assertEquals(42, GetRemainingCreditsUseCase(FakeCreditRepository { 42 })())
        }

    @Test
    fun `못 받으면 0 이 아니라 null 이다`() =
        runTest {
            val repository = FakeCreditRepository { throw ApiException.ServerException(rawCode = 500) }

            assertNull(GetRemainingCreditsUseCase(repository)())
        }

    private class FakeCreditRepository(
        private val remaining: suspend () -> Int,
    ) : CreditRepository {
        override suspend fun getRemainingCredits(): Int = remaining()

        override suspend fun getCosts(): CreditCosts = CreditCosts(timelineCreation = 1)
    }
}
