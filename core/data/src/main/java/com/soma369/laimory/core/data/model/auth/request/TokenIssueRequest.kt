package com.soma369.laimory.core.data.model.auth.request

import kotlinx.serialization.Serializable

@Serializable
data class TokenIssueRequest(
    val appCode: String,
    val appVerifier: String,
) {
    override fun toString(): String = "TokenIssueRequest(REDACTED)"
}
