package com.soma369.laimory.core.domain.usecase.auth

import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 현재 인증 세션에 연결된 로그인 계정 메타데이터를 관찰한다. */
class ObserveSignedInAccountUseCase
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        operator fun invoke(): Flow<SignedInAccount?> = repository.observeSignedInAccount()
    }
