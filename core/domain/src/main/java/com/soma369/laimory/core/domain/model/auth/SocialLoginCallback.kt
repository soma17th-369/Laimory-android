package com.soma369.laimory.core.domain.model.auth

/** 서버 OAuth가 앱의 HTTPS callback으로 전달한 결과. */
data class SocialLoginCallback(
    val appCode: String? = null,
    val errorCode: String? = null,
) {
    override fun toString(): String = "SocialLoginCallback(REDACTED)"
}
