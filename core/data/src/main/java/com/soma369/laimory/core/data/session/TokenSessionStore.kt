package com.soma369.laimory.core.data.session

import kotlinx.coroutines.flow.Flow

/** access/refresh token 쌍을 부분 갱신 없이 하나의 인증 세션 단위로 저장하는 계약. */
internal interface TokenSessionStore {
    /** 저장된 세션을 관찰하며 세션이 없거나 복호화할 수 없으면 `null`을 방출한다. */
    fun observe(): Flow<TokenSession?>

    /** 현재 세션을 반환하며 세션이 없거나 복호화할 수 없으면 `null`을 반환한다. */
    suspend fun get(): TokenSession?

    /** 기존 인증 세션 전체를 주어진 [session]으로 교체한다. */
    suspend fun save(session: TokenSession)

    /** 저장된 인증 세션 전체를 제거한다. */
    suspend fun clear()
}
