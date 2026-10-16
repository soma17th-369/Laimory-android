package com.soma369.laimory.feature.settings.credit

import com.soma369.laimory.core.domain.model.credit.TimelineCredit
import org.junit.Assert.assertEquals
import org.junit.Test

class CreditBalanceStoreTest {
    private val store = CreditBalanceStore()

    @Test
    fun `잔액과 비용을 받으면 함께 기억한다`() {
        store.updateCredit(store.generation, TimelineCredit(cost = 1, remaining = 42))

        assertEquals(CreditBalance(remaining = 42, cost = 1), store.balance.value)
    }

    @Test
    fun `잔액만 받으면 비용은 그대로 둔다`() {
        store.updateCredit(store.generation, TimelineCredit(cost = 1, remaining = 42))

        store.updateRemaining(store.generation, 41)

        assertEquals(CreditBalance(remaining = 41, cost = 1), store.balance.value)
    }

    @Test
    fun `못 받으면 잔액을 비운다`() {
        store.updateCredit(store.generation, TimelineCredit(cost = 1, remaining = 42))

        store.updateCredit(store.generation, null)

        assertEquals(CreditBalance(remaining = null, cost = 1), store.balance.value)
    }

    @Test
    fun `비운 뒤 이전 번호로 들어온 응답은 버린다`() {
        val before = store.generation
        store.updateRemaining(before, 42)

        store.clear()
        store.updateRemaining(before, 42)

        assertEquals(CreditBalance(), store.balance.value)
    }
}
