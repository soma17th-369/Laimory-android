package com.soma369.laimory.core.domain.repository

/**
 * 새 로그인 세션이 시작되기 직전에 계정에 딸린 로컬 값을 갈라 둔다.
 *
 * 로그인 토큰을 저장하기 **전에** 부른다(`IssueAuthTokensUseCase`). 로그아웃 순간을 관찰해 비우면, 인증 상태가
 * 로그아웃 → 로그인으로 빠르게 지나갈 때 중간 상태를 놓칠 수 있다. 로그인은 놓칠 수 없는 한 번의 호출이다.
 */
fun interface NewSessionPreparer {
    suspend fun prepareNewSession()
}
