package com.soma369.laimory.core.domain.usecase.auth

import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 앱 시작과 실행 중 인증 세션 상태를 관찰한다. */
@Singleton
class ObserveAuthSessionUseCase
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        operator fun invoke(): Flow<AuthSessionState> = repository.observeSessionState()
    }
