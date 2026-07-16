package com.soma369.laimory.core.domain.usecase.auth

import com.soma369.laimory.core.domain.repository.SocialLoginRepository
import javax.inject.Inject

/** 브라우저에서 callback 없이 돌아온 로그인 시도를 폐기한다. */
class CancelSocialLoginUseCase
    @Inject
    constructor(
        private val repository: SocialLoginRepository,
    ) {
        suspend operator fun invoke() = repository.clearPendingAttempt()
    }
