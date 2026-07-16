package com.soma369.laimory.core.domain.model.auth

/** 이 기기의 인증 세션에 연결된 최소 계정 정보. */
data class SignedInAccount(
    val provider: SocialLoginProvider?,
)
