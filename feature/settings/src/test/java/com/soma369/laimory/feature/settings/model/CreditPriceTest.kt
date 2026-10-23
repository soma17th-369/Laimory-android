package com.soma369.laimory.feature.settings.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreditPriceTest {
    @Test
    fun `가격은 천 단위 쉼표와 원을 붙인다`() {
        assertEquals("200원", 200.toWon())
        assertEquals("3,500원", 3_500.toWon())
    }

    @Test
    fun `1개 상품은 비교 기준이라 개당 가격을 붙이지 않는다`() {
        assertNull(unitPriceLabel(amount = 1, price = 200))
    }

    @Test
    fun `나누어떨어지지 않는 개당 가격은 반올림하고 약을 붙인다`() {
        assertEquals("개당 약 143원", unitPriceLabel(amount = 7, price = 1_000))
        assertEquals("개당 약 117원", unitPriceLabel(amount = 30, price = 3_500))
    }

    @Test
    fun `나누어떨어지면 약 없이 적는다`() {
        assertEquals("개당 100원", unitPriceLabel(amount = 10, price = 1_000))
    }
}
