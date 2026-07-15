package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.AuthRemoteDataSource
import com.soma369.laimory.core.data.session.AuthSessionOperationLock
import com.soma369.laimory.core.data.session.TokenSessionStore
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

internal class AuthRepositoryImpl
    @Inject
    constructor(
        private val remote: AuthRemoteDataSource,
        private val sessionStore: TokenSessionStore,
        private val operationLock: AuthSessionOperationLock,
    ) : AuthRepository {
        override fun observeSessionState(): Flow<AuthSessionState> =
            sessionStore.observe()
                .map { session ->
                    if (session == null) AuthSessionState.Unauthenticated else AuthSessionState.Authenticated
                }.onStart { emit(AuthSessionState.Loading) }

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ) = operationLock.mutex.withLock {
            val session = remote.issueTokens(appCode, appVerifier).toSession()
            sessionStore.save(session)
        }

        override suspend fun logout() =
            operationLock.mutex.withLock {
                val refreshToken = sessionStore.get()?.refreshToken
                try {
                    if (refreshToken != null) remote.logout(refreshToken)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // 사용자에게는 로컬 로그아웃 완료가 우선이며 서버 토큰은 만료로 소멸한다.
                } finally {
                    sessionStore.clear()
                }
            }
    }
