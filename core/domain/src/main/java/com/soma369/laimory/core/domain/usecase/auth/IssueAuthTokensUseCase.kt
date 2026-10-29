package com.soma369.laimory.core.domain.usecase.auth

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.NewSessionPreparer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그인 callback의 일회용 code와 verifier를 토큰 쌍으로 교환한다.
 *
 * 교환 전에 계정에 딸린 로컬 값을 새 세션 몫으로 갈라 둔다([NewSessionPreparer]). 토큰이 저장되는 순간 인증 상태가
 * 바뀌므로, 그보다 먼저여야 새 세션이 앞 계정의 값으로 판정받지 않는다.
 */
@Singleton
class IssueAuthTokensUseCase
    @Inject
    constructor(
        private val repository: AuthRepository,
        private val newSessionPreparer: NewSessionPreparer,
    ) {
        suspend operator fun invoke(params: IssueAuthTokensParams): Result<Unit> =
            try {
                newSessionPreparer.prepareNewSession()
                repository.issueTokens(params.appCode, params.appVerifier)
                Result.success(Unit)
            } catch (error: ApiException) {
                // -2002는 로그인 화면이 재시도 상태로 처리하므로 공통 세션 만료 메시지를 발행하지 않는다.
                Result.failure(error)
            }
    }
