package com.soma369.laimory.core.data.model.auth.response

import com.soma369.laimory.core.data.session.TokenSession
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
) {
    internal fun toSession(
        sessionId: String? = null,
        loginProvider: String? = null,
    ): TokenSession =
        TokenSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            sessionId = sessionId ?: java.util.UUID.randomUUID().toString(),
            loginProvider = loginProvider,
        )

    override fun toString(): String = "TokenResponse(REDACTED)"
}
