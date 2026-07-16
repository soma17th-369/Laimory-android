package com.soma369.laimory.core.domain.usecase.auth

import com.soma369.laimory.core.domain.repository.AuthRepository
import javax.inject.Inject

/** 서버 토큰 폐기를 시도한 뒤 이 기기의 인증 세션을 제거한다. */
class LogoutUseCase
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        suspend operator fun invoke() = repository.logout()
    }
