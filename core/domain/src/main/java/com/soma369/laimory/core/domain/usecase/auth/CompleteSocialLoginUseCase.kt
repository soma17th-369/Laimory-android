package com.soma369.laimory.core.domain.usecase.auth

import com.soma369.laimory.core.domain.exception.SocialLoginException
import com.soma369.laimory.core.domain.model.auth.SocialLoginCallback
import com.soma369.laimory.core.domain.repository.SocialLoginRepository
import javax.inject.Inject

/** HTTPS callback을 검증하고 보관된 verifier와 appCode를 토큰 쌍으로 교환한다. */
class CompleteSocialLoginUseCase
    @Inject
    constructor(
        private val socialLoginRepository: SocialLoginRepository,
        private val issueAuthTokens: IssueAuthTokensUseCase,
    ) {
        suspend operator fun invoke(callback: SocialLoginCallback): Result<Unit> {
            callback.errorCode?.takeIf(String::isNotBlank)?.let { errorCode ->
                socialLoginRepository.clearPendingAttempt()
                return Result.failure(SocialLoginException.ProviderFailure(errorCode))
            }

            val appCode = callback.appCode?.takeIf(String::isNotBlank)
            if (appCode == null) {
                socialLoginRepository.clearPendingAttempt()
                return Result.failure(SocialLoginException.InvalidCallback)
            }

            val verifier =
                socialLoginRepository.consumePendingVerifier()
                    ?: return Result.failure(SocialLoginException.MissingAttempt)

            return issueAuthTokens(IssueAuthTokensParams(appCode, verifier))
        }
    }
