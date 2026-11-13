package com.soma369.laimory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthDeepLinkTest {
    private val callbackHost = BuildConfig.AUTH_CALLBACK_HOST
    private val callbackOrigin = "https://$callbackHost"

    @Test
    fun `성공 callback의 code를 appCode로 변환한다`() {
        val callback = "$callbackOrigin/auth/app?code=one-time-code".toSocialLoginCallbackOrNull(callbackHost)

        assertEquals("one-time-code", callback?.appCode)
        assertNull(callback?.errorCode)
    }

    @Test
    fun `실패 callback의 error를 변환한다`() {
        val callback = "$callbackOrigin/auth/app?error=-2004".toSocialLoginCallbackOrNull(callbackHost)

        assertEquals("-2004", callback?.errorCode)
        assertNull(callback?.appCode)
    }

    @Test
    fun `다른 scheme host path는 callback으로 처리하지 않는다`() {
        assertNull("http://$callbackHost/auth/app?code=x".toSocialLoginCallbackOrNull(callbackHost))
        assertNull("https://example.com/auth/app?code=x".toSocialLoginCallbackOrNull(callbackHost))
        assertNull("$callbackOrigin/auth/other?code=x".toSocialLoginCallbackOrNull(callbackHost))
    }

    @Test
    fun `올바른 callback path의 빈 query도 잘못된 결과로 전달한다`() {
        val callback = "$callbackOrigin/auth/app".toSocialLoginCallbackOrNull(callbackHost)

        assertEquals(null, callback?.appCode)
        assertEquals(null, callback?.errorCode)
    }

    @Test
    fun `다른 서버를 고르면 그 서버 host 의 callback 만 받는다`() {
        val apiHost = authCallbackHostOf("https://api.laimory.app/")

        assertEquals("api.laimory.app", apiHost)
        assertEquals("x", "https://api.laimory.app/auth/app?code=x".toSocialLoginCallbackOrNull(apiHost)?.appCode)
        assertNull("$callbackOrigin/auth/app?code=x".toSocialLoginCallbackOrNull(apiHost))
    }
}
