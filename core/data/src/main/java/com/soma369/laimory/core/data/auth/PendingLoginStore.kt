package com.soma369.laimory.core.data.auth

/** OAuth callback까지 필요한 verifier를 프로세스 종료와 무관하게 보관한다. */
internal interface PendingLoginStore {
    suspend fun save(verifier: String)

    suspend fun consume(): String?

    suspend fun clear()
}
