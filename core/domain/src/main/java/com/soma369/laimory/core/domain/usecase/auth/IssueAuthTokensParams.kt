package com.soma369.laimory.core.domain.usecase.auth

data class IssueAuthTokensParams(
    val appCode: String,
    val appVerifier: String,
) {
    init {
        require(appCode.isNotBlank()) { "App code must not be blank" }
        require(appVerifier.isNotBlank()) { "App verifier must not be blank" }
    }

    override fun toString(): String = "IssueAuthTokensParams(REDACTED)"
}
