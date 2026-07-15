package com.soma369.laimory.core.data.model.auth.request

import kotlinx.serialization.Serializable

@Serializable
data class LogoutRequest(
    val refreshToken: String,
) {
    override fun toString(): String = "LogoutRequest(REDACTED)"
}
