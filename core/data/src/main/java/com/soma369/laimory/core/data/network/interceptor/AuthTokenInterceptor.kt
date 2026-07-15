package com.soma369.laimory.core.data.network.interceptor

import com.soma369.laimory.core.data.session.TokenSessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/** 보호 API 요청에만 현재 access token을 Bearer header로 첨부한다. */
@Singleton
internal class AuthTokenInterceptor
    @Inject
    constructor(
        private val sessionStore: TokenSessionStore,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (request.header(AUTHORIZATION) != null) return chain.proceed(request)

            val session = runBlocking { sessionStore.get() }
            val authenticatedRequest =
                if (session == null) {
                    request
                } else {
                    request.newBuilder()
                        .header(AUTHORIZATION, "$BEARER_PREFIX${session.accessToken}")
                        .tag(AuthSessionRequestTag::class.java, AuthSessionRequestTag(session.sessionId))
                        .build()
                }
            return chain.proceed(authenticatedRequest)
        }

        internal companion object {
            const val AUTHORIZATION = "Authorization"
            const val BEARER_PREFIX = "Bearer "
        }
    }

internal data class AuthSessionRequestTag(
    val sessionId: String,
)
