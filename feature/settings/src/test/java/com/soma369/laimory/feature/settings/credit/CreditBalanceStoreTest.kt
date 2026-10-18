package com.soma369.laimory.feature.settings.credit

import com.soma369.laimory.core.domain.model.credit.TimelineCredit
import org.junit.Assert.assertEquals
import org.junit.Test

class CreditBalanceStoreTest {
    private val store = CreditBalanceStore()

    @Test
    fun `잔액과 비용을 받으면 함께 기억한다`() {
        store.updateCredit(store.beginRequest(), TimelineCredit(cost = 1, remaining = 42))

        assertEquals(CreditBalance(remaining = 42, cost = 1), store.balance.value)
    }

    @Test
    fun `잔액만 받으면 비용은 그대로 둔다`() {
        store.updateCredit(store.beginRequest(), TimelineCredit(cost = 1, remaining = 42))

        store.updateRemaining(store.beginRequest(), 41)

        assertEquals(CreditBalance(remaining = 41, cost = 1), store.balance.value)
    }

    @Test
    fun `못 받으면 잔액을 비운다`() {
        store.updateCredit(store.beginRequest(), TimelineCredit(cost = 1, remaining = 42))

        store.updateCredit(store.beginRequest(), null)

        assertEquals(CreditBalance(remaining = null, cost = 1), store.balance.value)
    }

    @Test
    fun `나중에 보낸 요청의 응답을 반영했으면 먼저 보낸 요청의 응답은 버린다`() {
        // 설정이 먼저 보낸 느린 조회(42)가 그 뒤 크레딧 화면이 받은 최신 잔액(41)을 덮으면 안 된다.
        val fromSettings = store.beginRequest()
        val fromCredit = store.beginRequest()
        store.updateCredit(fromCredit, TimelineCredit(cost = 1, remaining = 41))

        store.updateRemaining(fromSettings, 42)
        store.updateRemaining(fromSettings, null)

        assertEquals(CreditBalance(remaining = 41, cost = 1), store.balance.value)
    }

    @Test
    fun `먼저 보낸 요청이 먼저 오면 반영하고 나중 요청의 응답으로 다시 바꾼다`() {
        val first = store.beginRequest()
        val second = store.beginRequest()

        store.updateRemaining(first, 42)
        assertEquals(42, store.balance.value.remaining)
        store.updateRemaining(second, 41)

        assertEquals(41, store.balance.value.remaining)
    }

    @Test
    fun `계정 경계에서 비운 뒤에는 그 전에 보낸 요청의 응답을 모두 버린다`() {
        val before = store.beginRequest()

        store.clear()
        store.updateRemaining(before, 42)

        assertEquals(CreditBalance(), store.balance.value)
        val after = store.beginRequest()
        store.updateRemaining(after, 7)
        assertEquals(7, store.balance.value.remaining)
    }
}
