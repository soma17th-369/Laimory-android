package com.soma369.laimory.crash

import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그인 상태를 크래시 리포트의 꼬리표로 유지한다.
 *
 * 계정 자체는 넣지 않는다. 회원 식별자를 리포트에 실으면 크래시 데이터가 개인을 지목할 수 있게
 * 된다. 로그인 전후 어느 쪽에서 나는 크래시인지만 가르면 원인 좁히기에는 충분하다.
 */
@Singleton
class SignedInCrashKeyObserver
    @Inject
    constructor(
        private val observeSignedInAccount: ObserveSignedInAccountUseCase,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) {
        private var sessionJob: Job? = null

        fun start() {
            if (sessionJob?.isActive == true) return
            sessionJob =
                applicationScope.launch {
                    observeSignedInAccount().collect { account ->
                        Logger.setCrashKey(CrashKey.SIGNED_IN, (account != null).toString())
                    }
                }
        }
    }
