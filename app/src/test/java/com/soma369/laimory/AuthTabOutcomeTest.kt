package com.soma369.laimory

import androidx.browser.auth.AuthTabIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthTabOutcomeTest {
    private val authorizationUrl = "https://api.laimory.app/oauth2/authorization/google?challenge=x"
    private val callbackUri = "https://${BuildConfig.AUTH_CALLBACK_HOST}/auth/app?code=one-time-code"

    @Test
    fun `성공하면 돌아온 콜백 주소를 넘긴다`() {
        val outcome = authTabOutcome(AuthTabIntent.RESULT_OK, callbackUri, authorizationUrl)

        assertEquals(AuthTabOutcome.Deliver(callbackUri), outcome)
    }

    @Test
    fun `성공인데 주소가 없으면 실패로 본다`() {
        val outcome = authTabOutcome(AuthTabIntent.RESULT_OK, null, authorizationUrl)

        assertTrue(outcome is AuthTabOutcome.Failed)
    }

    @Test
    fun `닫히면 아무것도 다시 열지 않는다`() {
        val outcome = authTabOutcome(AuthTabIntent.RESULT_CANCELED, null, authorizationUrl)

        assertEquals(AuthTabOutcome.Closed, outcome)
    }

    @Test
    fun `소유 확인에 실패하면 같은 인증 주소를 다시 연다`() {
        val outcome = authTabOutcome(AuthTabIntent.RESULT_VERIFICATION_FAILED, null, authorizationUrl)

        assertEquals(authorizationUrl, (outcome as AuthTabOutcome.Reopen).authorizationUrl)
    }

    @Test
    fun `소유 확인 시간이 초과돼도 같은 인증 주소를 다시 연다`() {
        val outcome = authTabOutcome(AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT, null, authorizationUrl)

        assertEquals(authorizationUrl, (outcome as AuthTabOutcome.Reopen).authorizationUrl)
    }

    @Test
    fun `다시 열 인증 주소를 잃었으면 실패로 본다`() {
        val outcome = authTabOutcome(AuthTabIntent.RESULT_VERIFICATION_FAILED, null, authorizationUrl = null)

        assertTrue(outcome is AuthTabOutcome.Failed)
    }

    @Test
    fun `모르는 결과는 다시 열지 않고 실패로 본다`() {
        val outcome = authTabOutcome(AuthTabIntent.RESULT_UNKNOWN_CODE, null, authorizationUrl)

        assertTrue(outcome is AuthTabOutcome.Failed)
    }
}
