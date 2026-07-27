package com.soma369.laimory.push

import com.google.firebase.messaging.FirebaseMessaging
import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** 앱 프로세스에서 인증 세션을 관찰하고 로그인 직후 FCM onRegistered 콜백을 유도한다. */
@Singleton
class PushRegistrationSessionObserver
    @Inject
    constructor(
        private val observeAuthSession: ObserveAuthSessionUseCase,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) {
        private var sessionJob: Job? = null

        fun start() {
            if (sessionJob?.isActive == true) return
            sessionJob =
                applicationScope.launch {
                    observeAuthSession().collect { state ->
                        if (state != AuthSessionState.Authenticated) return@collect
                        FirebaseMessaging.getInstance().register()
                    }
                }
        }
    }
