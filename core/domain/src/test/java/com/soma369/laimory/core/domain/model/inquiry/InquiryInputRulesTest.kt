package com.soma369.laimory.core.domain.model.inquiry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InquiryInputRulesTest {
    @Test
    fun `앞뒤 공백을 뺀 주소로 형식을 본다`() {
        assertTrue(InquiryInputRules.isValidEmail("  user@example.com "))
    }

    @Test
    fun `답장을 받을 수 없는 주소는 거른다`() {
        listOf("", "user", "user@", "@example.com", "user@example", "us er@example.com").forEach { email ->
            assertFalse(email, InquiryInputRules.isValidEmail(email))
        }
    }

    @Test
    fun `주소는 255자까지다`() {
        val local = "a".repeat(255 - "@example.com".length)
        assertTrue(InquiryInputRules.isValidEmail("$local@example.com"))
        assertFalse(InquiryInputRules.isValidEmail("a$local@example.com"))
    }

    @Test
    fun `본문은 공백만은 안 되고 줄바꿈을 포함해 2000자까지다`() {
        assertFalse(InquiryInputRules.isValidBody(" \n "))
        assertTrue(InquiryInputRules.isValidBody("가\n".repeat(1_000)))
        assertFalse(InquiryInputRules.isValidBody("가\n".repeat(1_000) + "나"))
    }

    @Test
    fun `첨부는 3장까지다`() {
        val base = InquirySubmission(email = "user@example.com", body = "문의")
        assertTrue(base.copy(attachmentUris = List(3) { "content://photo/$it" }).isValid)
        assertFalse(base.copy(attachmentUris = List(4) { "content://photo/$it" }).isValid)
    }
}
