package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.auth.OAuthAuthorizationUrlFactory
import com.soma369.laimory.core.data.auth.PendingLoginStore
import com.soma369.laimory.core.data.auth.PkceGenerator
import com.soma369.laimory.core.domain.model.auth.SocialLoginAttempt
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.repository.SocialLoginRepository
import javax.inject.Inject

internal class SocialLoginRepositoryImpl
    @Inject
    constructor(
        private val pkceGenerator: PkceGenerator,
        private val pendingLoginStore: PendingLoginStore,
        private val urlFactory: OAuthAuthorizationUrlFactory,
    ) : SocialLoginRepository {
        override suspend fun start(provider: SocialLoginProvider): SocialLoginAttempt {
            val pkce = pkceGenerator.generate()
            // save가 기존 값을 원자적으로 덮어써 이전 미완료 시도를 폐기한다.
            pendingLoginStore.save(pkce.verifier)
            return SocialLoginAttempt(urlFactory.create(provider, pkce.challenge))
        }

        override suspend fun consumePendingVerifier(): String? = pendingLoginStore.consume()

        override suspend fun clearPendingAttempt() = pendingLoginStore.clear()
    }
