package com.soma369.laimory.core.domain.usecase.auth

import com.soma369.laimory.core.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/** 로그인 callback의 일회용 code와 verifier를 토큰 쌍으로 교환한다. */
@Singleton
class IssueAuthTokensUseCase
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        suspend operator fun invoke(params: IssueAuthTokensParams): Result<Unit> =
            try {
                repository.issueTokens(params.appCode, params.appVerifier)
                Result.success(Unit)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // ERROR_2002는 로그인 화면이 재시도 상태로 처리하므로 공통 세션 만료 메시지를 발행하지 않는다.
                Result.failure(error)
            }
    }
