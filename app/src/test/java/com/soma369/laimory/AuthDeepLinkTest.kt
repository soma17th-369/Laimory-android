package com.soma369.laimory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthDeepLinkTest {
    private val callbackOrigin = "https://${BuildConfig.AUTH_CALLBACK_HOST}"

    @Test
    fun `성공 callback의 code를 appCode로 변환한다`() {
        val callback = "$callbackOrigin/auth/app?code=one-time-code".toSocialLoginCallbackOrNull()

        assertEquals("one-time-code", callback?.appCode)
        assertNull(callback?.errorCode)
    }

    @Test
    fun `실패 callback의 error를 변환한다`() {
        val callback = "$callbackOrigin/auth/app?error=-2004".toSocialLoginCallbackOrNull()

        assertEquals("-2004", callback?.errorCode)
        assertNull(callback?.appCode)
    }

    @Test
    fun `다른 scheme host path는 callback으로 처리하지 않는다`() {
        assertNull("http://${BuildConfig.AUTH_CALLBACK_HOST}/auth/app?code=x".toSocialLoginCallbackOrNull())
        assertNull("https://example.com/auth/app?code=x".toSocialLoginCallbackOrNull())
        assertNull("$callbackOrigin/auth/other?code=x".toSocialLoginCallbackOrNull())
    }

    @Test
    fun `올바른 callback path의 빈 query도 잘못된 결과로 전달한다`() {
        val callback = "$callbackOrigin/auth/app".toSocialLoginCallbackOrNull()

        assertEquals(null, callback?.appCode)
        assertEquals(null, callback?.errorCode)
    }
}
