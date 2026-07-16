package com.soma369.laimory.core.data.session

import kotlinx.serialization.Serializable
import java.util.UUID

/** 네트워크 계층 내부에서만 사용하는 토큰 쌍. 문자열 노출을 막기 위해 data class로 만들지 않는다. */
@Serializable
internal class TokenSession(
    val accessToken: String,
    val refreshToken: String,
    val sessionId: String = UUID.randomUUID().toString(),
    val loginProvider: String? = null,
) {
    init {
        require(accessToken.isNotBlank()) { "Access token must not be blank" }
        require(refreshToken.isNotBlank()) { "Refresh token must not be blank" }
        require(sessionId.isNotBlank()) { "Session id must not be blank" }
    }

    override fun toString(): String = "TokenSession(REDACTED)"
}
