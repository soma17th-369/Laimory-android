package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.auth.request.LogoutRequest
import com.soma369.laimory.core.data.model.auth.request.TokenIssueRequest
import com.soma369.laimory.core.data.model.auth.request.TokenRefreshRequest
import com.soma369.laimory.core.data.model.auth.response.TokenResponse
import com.soma369.laimory.core.data.network.api.AuthApi
import com.soma369.laimory.core.data.network.safeApiCall
import com.soma369.laimory.core.data.network.safeApiCallUnit
import javax.inject.Inject

class AuthRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: AuthApi,
    ) : AuthRemoteDataSource {
        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ): TokenResponse =
            safeApiCall {
                api.issueTokens(
                    TokenIssueRequest(
                        appCode = appCode,
                        appVerifier = appVerifier,
                    ),
                )
            }

        override suspend fun refreshTokens(refreshToken: String): TokenResponse =
            safeApiCall { api.refreshTokens(TokenRefreshRequest(refreshToken)) }

        override suspend fun logout(refreshToken: String) {
            safeApiCallUnit { api.logout(LogoutRequest(refreshToken)) }
        }
    }
