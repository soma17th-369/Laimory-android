package com.soma369.laimory.feature.settings.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CreditCountFormatTest {
    @Test
    fun `세 자리까지는 그대로 보인다`() {
        assertEquals("0", 0.toCreditCount())
        assertEquals("42", 42.toCreditCount())
        assertEquals("999", 999.toCreditCount())
    }

    @Test
    fun `네 자리부터는 천 단위 쉼표를 찍고 자르지 않는다`() {
        assertEquals("1,000", 1_000.toCreditCount())
        assertEquals("12,345", 12_345.toCreditCount())
        assertEquals("1,234,567", 1_234_567.toCreditCount())
    }
}
