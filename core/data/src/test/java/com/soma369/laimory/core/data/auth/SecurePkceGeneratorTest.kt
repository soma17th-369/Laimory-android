package com.soma369.laimory.core.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurePkceGeneratorTest {
    @Test
    fun `RFC 7636 S256 예제와 같은 challenge를 만든다`() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

        val challenge = SecurePkceGenerator.challengeOf(verifier)

        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", challenge)
    }

    @Test
    fun `매 시도마다 URL safe 43자 verifier와 challenge를 만든다`() {
        val generator = SecurePkceGenerator()

        val first = generator.generate()
        val second = generator.generate()

        assertTrue(first.verifier.matches(PKCE_PATTERN))
        assertTrue(first.challenge.matches(PKCE_PATTERN))
        assertTrue(first.verifier != second.verifier)
    }

    private companion object {
        val PKCE_PATTERN = Regex("[A-Za-z0-9_-]{43}")
    }
}
