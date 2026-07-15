package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.auth.request.LogoutRequest
import com.soma369.laimory.core.data.model.auth.request.TokenIssueRequest
import com.soma369.laimory.core.data.model.auth.request.TokenRefreshRequest
import com.soma369.laimory.core.data.model.auth.response.TokenResponse
import com.soma369.laimory.core.data.model.common.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** 인증 전 public prefix의 민감 토큰 API. BODY 로깅과 Bearer refresh 대상에서 제외한다. */
interface AuthApi {
    /** 일회용 app code와 verifier를 토큰 쌍으로 교환한다. */
    @POST("auth/token")
    suspend fun issueTokens(
        @Body request: TokenIssueRequest,
    ): Response<ApiResponse<TokenResponse>>

    /** refresh token을 새 access/refresh token 쌍으로 교환한다. */
    @POST("auth/refresh")
    suspend fun refreshTokens(
        @Body request: TokenRefreshRequest,
    ): Response<ApiResponse<TokenResponse>>

    /** refresh token 폐기를 요청한다. 성공 여부만 사용하며 응답 데이터는 없다. */
    @POST("auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest,
    ): Response<ApiResponse<Unit>>
}
