package com.soma369.laimory.core.domain.usecase.auth

import com.soma369.laimory.core.domain.model.auth.SocialLoginAttempt
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.repository.SocialLoginRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/** 새 PKCE 시도를 만들고 Custom Tab에서 열 서버 OAuth 주소를 준비한다. */
class StartSocialLoginUseCase
    @Inject
    constructor(
        private val repository: SocialLoginRepository,
    ) {
        suspend operator fun invoke(provider: SocialLoginProvider): Result<SocialLoginAttempt> =
            try {
                Result.success(repository.start(provider))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
    }
