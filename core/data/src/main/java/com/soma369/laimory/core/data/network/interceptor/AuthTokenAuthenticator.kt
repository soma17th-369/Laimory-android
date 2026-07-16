package com.soma369.laimory.core.data.network.interceptor

import com.soma369.laimory.core.data.datasource.remote.AuthRemoteDataSource
import com.soma369.laimory.core.data.session.AuthSessionOperationLock
import com.soma369.laimory.core.data.session.TokenSessionStore
import com.soma369.laimory.core.domain.exception.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** HTTP 401을 한 번의 refresh rotation으로 복구하고 동시 요청을 single-flight로 합친다. */
@Singleton
internal class AuthTokenAuthenticator
    @Inject
    constructor(
        private val sessionStore: TokenSessionStore,
        private val remote: AuthRemoteDataSource,
        private val operationLock: AuthSessionOperationLock,
    ) : Authenticator {
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            if (response.retryCount() >= MAX_REQUEST_COUNT) return null
            val failedAuthorization = response.request.header(AuthTokenInterceptor.AUTHORIZATION) ?: return null
            val failedSessionId = response.request.tag(AuthSessionRequestTag::class.java)?.sessionId ?: return null

            return runBlocking {
                operationLock.mutex.withLock {
                    val currentSession = sessionStore.get() ?: return@withLock null
                    // 로그아웃 뒤 새 계정으로 로그인했다면 과거 요청을 새 세션으로 재전송하지 않는다.
                    if (failedSessionId != currentSession.sessionId) return@withLock null
                    val currentAuthorization = AuthTokenInterceptor.BEARER_PREFIX + currentSession.accessToken

                    // 다른 요청이 먼저 refresh를 끝냈으면 회전된 access token만 재사용한다.
                    if (failedAuthorization != currentAuthorization) {
                        return@withLock response.request.withBearer(currentSession.accessToken)
                    }

                    val refreshed =
                        try {
                            remote.refreshTokens(currentSession.refreshToken).toSession(
                                sessionId = currentSession.sessionId,
                                loginProvider = currentSession.loginProvider,
                            )
                        } catch (error: ApiException.UnauthorizedException) {
                            if (error.errorCode == REFRESH_REJECTED || error.rawCode == 401) {
                                sessionStore.clear()
                            }
                            return@withLock null
                        } catch (error: CancellationException) {
                            throw IOException("Token refresh cancelled", error)
                        } catch (error: ApiException) {
                            // 일시 네트워크/서버 실패에는 아직 유효할 수 있는 refresh를 지우지 않는다.
                            throw IOException("Token refresh failed", error)
                        }

                    try {
                        sessionStore.save(refreshed)
                    } catch (error: Exception) {
                        // 서버에서는 이미 회전됐으므로 새 쌍을 저장하지 못하면 재로그인이 안전하다.
                        runCatching { sessionStore.clear() }
                        throw IOException("Rotated token persistence failed", error)
                    }
                    response.request.withBearer(refreshed.accessToken)
                }
            }
        }

        private fun Request.withBearer(accessToken: String): Request =
            newBuilder()
                .header(
                    AuthTokenInterceptor.AUTHORIZATION,
                    AuthTokenInterceptor.BEARER_PREFIX + accessToken,
                ).build()

        private fun Response.retryCount(): Int {
            var count = 1
            var previous = priorResponse
            while (previous != null) {
                count++
                previous = previous.priorResponse
            }
            return count
        }

        private companion object {
            const val REFRESH_REJECTED = "ERROR_2003"
            const val MAX_REQUEST_COUNT = 2
        }
    }
