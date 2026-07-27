package com.soma369.laimory.core.domain.usecase.auth

import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.usecase.push.UnregisterCurrentPushInstallationUseCase
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** 현재 FID를 best-effort로 해제한 뒤 서버 토큰과 이 기기의 인증 세션을 제거한다. */
class LogoutUseCase
    @Inject
    constructor(
        private val repository: AuthRepository,
        private val unregisterCurrentPushInstallation: UnregisterCurrentPushInstallationUseCase,
    ) {
        suspend operator fun invoke() {
            // FID DELETE는 Bearer access token이 필요한 요청이라 세션을 비우기 전에 실행한다.
            withTimeoutOrNull(PUSH_UNREGISTER_TIMEOUT_MILLIS) {
                unregisterCurrentPushInstallation()
            }
            repository.logout()
        }

        private companion object {
            const val PUSH_UNREGISTER_TIMEOUT_MILLIS = 3_000L
        }
    }
