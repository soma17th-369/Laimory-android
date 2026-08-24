package com.soma369.laimory.core.domain.usecase.user

import com.soma369.laimory.core.domain.model.user.AccountWithdrawalOutcome
import com.soma369.laimory.core.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * 회원 탈퇴를 요청하고, 결과가 terminal 이면 이 기기의 인증 세션을 정리한다.
 *
 * [RequestAccountWithdrawalUseCase] 는 서버 계약만 다루고, 세션 정리는 여기서 한다.
 * [com.soma369.laimory.core.domain.usecase.auth.LogoutUseCase] 와 같은 층위의 오케스트레이션이다.
 *
 * 두 terminal 결과 모두 세션을 정리한다. `202` 는 서버가 credential 을 이미 차단했고,
 * [AccountWithdrawalOutcome.SessionUnavailable] 은 그 토큰으로 더 할 수 있는 일이 없다 —
 * 어느 쪽이든 로컬에 남겨 두면 모든 요청이 `401` 로 실패하는 죽은 세션이 된다.
 *
 * push 등록 해제는 하지 않는다. 서버가 탈퇴 접수 transaction 에서 해당 subject 의 등록을 모두
 * 지우고, 그 뒤 access token 은 `401` 이라 기기에서 보내는 해제 요청이 성공할 수 없다.
 * 로컬 등록 상태는 세션 전이를 관찰하는 쪽이 정리한다.
 *
 * 세션 정리는 요청 결과를 잃지 않도록 [Result] 성공 경로에서만 수행하며, 정리 자체가 실패하면
 * 그 실패를 그대로 올린다 — 죽은 세션이 남았는데 탈퇴 완료로 보이면 안 된다.
 */
class WithdrawAccountUseCase
    @Inject
    constructor(
        private val requestAccountWithdrawal: RequestAccountWithdrawalUseCase,
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(): Result<AccountWithdrawalOutcome> =
            requestAccountWithdrawal().mapCatching { outcome ->
                authRepository.clearSession()
                outcome
            }
    }
