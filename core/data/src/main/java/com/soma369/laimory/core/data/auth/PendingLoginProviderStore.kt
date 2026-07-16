package com.soma369.laimory.core.data.auth

import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider

/** 토큰 발급이 끝날 때까지 로그인 시작 제공자를 보관한다. */
internal interface PendingLoginProviderStore {
    suspend fun save(provider: SocialLoginProvider)

    suspend fun get(): SocialLoginProvider?

    suspend fun clear()
}
