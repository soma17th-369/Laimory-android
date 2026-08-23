package com.soma369.laimory.core.domain.usecase.user

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.user.AccountWithdrawalOutcome
import com.soma369.laimory.core.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 현재 인증 세션 계정의 탈퇴를 서버에 요청한다.
 *
 * 공통 [com.soma369.laimory.core.domain.base.BaseUseCase] 를 쓰지 않는다. 그쪽은 `401` 에
 * `UserMessage.SessionExpired`("세션이 만료되었습니다")를 발행하는데, 탈퇴 직후의 `401` 은
 * 만료가 아니라 계정이 사라진 결과라 틀린 문장이 뜬다. [GetUserProfileUseCase] 가 다른 이유로
 * 같은 선택을 한 선례가 있다.
 *
 * `401` 만 [AccountWithdrawalOutcome.SessionUnavailable] 로 수렴시키고, 네트워크 오류와 나머지
 * 4xx/5xx 는 기존 공통 [ApiException] 정책 그대로 실패로 넘긴다 — 탈퇴 전용 예외 코드를 만들지 않는다.
 */
class RequestAccountWithdrawalUseCase
    @Inject
    constructor(
        private val repository: UserRepository,
    ) {
        suspend operator fun invoke(): Result<AccountWithdrawalOutcome> =
            try {
                repository.requestAccountWithdrawal()
                Result.success(AccountWithdrawalOutcome.Accepted)
            } catch (e: ApiException) {
                if (e.rawCode == UNAUTHORIZED_CODE) {
                    Result.success(AccountWithdrawalOutcome.SessionUnavailable)
                } else {
                    Result.failure(e)
                }
            }

        private companion object {
            const val UNAUTHORIZED_CODE = 401
        }
    }
