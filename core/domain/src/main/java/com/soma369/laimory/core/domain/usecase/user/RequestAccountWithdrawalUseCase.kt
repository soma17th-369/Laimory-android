package com.soma369.laimory.core.domain.usecase.user

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.user.AccountWithdrawalOutcome
import com.soma369.laimory.core.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 현재 인증 세션 계정의 탈퇴를 서버에 요청한다.
 *
 * `401` 만 [AccountWithdrawalOutcome.SessionUnavailable] 로 수렴시켜 호출부가
 * `ApiException.rawCode` 를 직접 해석하지 않게 한다. 나머지 실패는 그대로 다시 던져
 * [BaseUseCase] 의 공통 정책으로 보낸다 — 404 는 `UnsupportedFeature`, 5xx 는
 * `TemporaryUnavailable` 이 발행되어야 하고, 탈퇴 전용 예외 코드를 만들지 않는다.
 *
 * 변환을 [BaseUseCase.execute] **안에서** 하는 것이 핵심이다. 401 이 공통 정책에 닿으면
 * `UserMessage.SessionExpired`("세션이 만료되었습니다")가 발행되는데, 탈퇴 직후의 401 은
 * 만료가 아니라 계정이 사라진 결과라 틀린 문장이 된다.
 */
class RequestAccountWithdrawalUseCase
    @Inject
    constructor(
        private val repository: UserRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(): Result<AccountWithdrawalOutcome> =
            execute {
                try {
                    repository.requestAccountWithdrawal()
                    AccountWithdrawalOutcome.Accepted
                } catch (e: ApiException) {
                    if (e.rawCode == UNAUTHORIZED_CODE) AccountWithdrawalOutcome.SessionUnavailable else throw e
                }
            }

        private companion object {
            const val UNAUTHORIZED_CODE = 401
        }
    }
