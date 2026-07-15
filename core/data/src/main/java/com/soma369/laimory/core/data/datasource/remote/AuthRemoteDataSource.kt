package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.auth.response.TokenResponse

/** 인증 세션 public API를 호출하고 공통 응답 envelope를 인증 응답 모델로 변환하는 계약. */
interface AuthRemoteDataSource {
    /** 로그인 callback의 일회용 app code와 verifier를 새 토큰 쌍으로 교환한다. */
    suspend fun issueTokens(
        appCode: String,
        appVerifier: String,
    ): TokenResponse

    /** 현재 refresh token을 제시하고 새 access/refresh token 쌍을 발급받는다. */
    suspend fun refreshTokens(refreshToken: String): TokenResponse

    /** 서버에 refresh token 폐기를 요청한다. 성공 응답의 body는 사용하지 않는다. */
    suspend fun logout(refreshToken: String)
}
