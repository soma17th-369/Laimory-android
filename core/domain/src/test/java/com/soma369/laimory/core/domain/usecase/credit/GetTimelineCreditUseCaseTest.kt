package com.soma369.laimory.core.domain.usecase.credit

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.credit.CreditCosts
import com.soma369.laimory.core.domain.model.credit.TimelineCredit
import com.soma369.laimory.core.domain.repository.CreditRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetTimelineCreditUseCaseTest {
    @Test
    fun `비용과 잔액을 함께 돌려준다`() =
        runTest {
            val credit = GetTimelineCreditUseCase(FakeCreditRepository(remaining = { 42 }, cost = { 1 }))()

            assertEquals(TimelineCredit(cost = 1, remaining = 42), credit)
        }

    @Test
    fun `잔액을 못 받으면 null 이다`() =
        runTest {
            // 크레딧 행이 없는 500 · 운영 서버에 API 가 없는 404 — 어느 쪽이든 줄을 숨기고 만들기는 막지 않는다.
            val repository = FakeCreditRepository(remaining = { throw ApiException.ServerException(rawCode = 500) }, cost = { 1 })

            assertNull(GetTimelineCreditUseCase(repository)())
        }

    @Test
    fun `비용을 못 받으면 null 이다`() =
        runTest {
            val repository = FakeCreditRepository(remaining = { 42 }, cost = { throw ApiException.NetworkException() })

            assertNull(GetTimelineCreditUseCase(repository)())
        }

    @Test
    fun `3초 안에 못 받으면 null 이다`() =
        runTest {
            val repository =
                FakeCreditRepository(
                    remaining = {
                        delay(5_000)
                        42
                    },
                    cost = { 1 },
                )

            assertNull(GetTimelineCreditUseCase(repository)())
        }

    @Test
    fun `잔액이 비용보다 적을 때만 부족하다`() {
        assertTrue(TimelineCredit(cost = 1, remaining = 1).isEnough)
        assertFalse(TimelineCredit(cost = 1, remaining = 0).isEnough)
        assertFalse(TimelineCredit(cost = 2, remaining = 1).isEnough)
    }

    private class FakeCreditRepository(
        private val remaining: suspend () -> Int,
        private val cost: suspend () -> Int,
    ) : CreditRepository {
        override suspend fun getRemainingCredits(): Int = remaining()

        override suspend fun getCosts(): CreditCosts = CreditCosts(timelineCreation = cost())
    }
}
