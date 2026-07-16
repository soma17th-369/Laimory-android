package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.auth.SocialLoginAttempt
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider

/** PKCE 로그인 시도를 생성하고 callback까지 필요한 verifier의 수명을 관리한다. */
interface SocialLoginRepository {
    /** 이전 미완료 시도를 폐기하고 새 verifier/challenge를 생성해 서버 OAuth 주소를 반환한다. */
    suspend fun start(provider: SocialLoginProvider): SocialLoginAttempt

    /** 보관된 verifier를 한 번만 반환하고 즉시 제거한다. */
    suspend fun consumePendingVerifier(): String?

    /** 취소·실패·잘못된 callback에서 남은 로그인 시도를 폐기한다. */
    suspend fun clearPendingAttempt()
}
