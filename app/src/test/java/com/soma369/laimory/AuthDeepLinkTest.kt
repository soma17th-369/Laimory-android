package com.soma369.laimory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthDeepLinkTest {
    @Test
    fun `성공 callback의 code를 appCode로 변환한다`() {
        val callback = "https://dev.laimory.app/auth/app?code=one-time-code".toSocialLoginCallbackOrNull()

        assertEquals("one-time-code", callback?.appCode)
        assertNull(callback?.errorCode)
    }

    @Test
    fun `실패 callback의 error를 변환한다`() {
        val callback = "https://dev.laimory.app/auth/app?error=ERROR_2004".toSocialLoginCallbackOrNull()

        assertEquals("ERROR_2004", callback?.errorCode)
        assertNull(callback?.appCode)
    }

    @Test
    fun `다른 scheme host path는 callback으로 처리하지 않는다`() {
        assertNull("http://dev.laimory.app/auth/app?code=x".toSocialLoginCallbackOrNull())
        assertNull("https://example.com/auth/app?code=x".toSocialLoginCallbackOrNull())
        assertNull("https://dev.laimory.app/auth/other?code=x".toSocialLoginCallbackOrNull())
    }

    @Test
    fun `올바른 callback path의 빈 query도 잘못된 결과로 전달한다`() {
        val callback = "https://dev.laimory.app/auth/app".toSocialLoginCallbackOrNull()

        assertEquals(null, callback?.appCode)
        assertEquals(null, callback?.errorCode)
    }
}
