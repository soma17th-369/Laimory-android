package com.soma369.laimory.core.domain.model.auth

/** 새 PKCE 시도를 저장한 뒤 브라우저에서 열 서버 OAuth 주소. */
data class SocialLoginAttempt(
    val authorizationUrl: String,
)
