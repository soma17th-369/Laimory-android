package com.soma369.laimory.draft

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DraftTaskProcessLifecycleObserver
    @Inject
    constructor(
        private val coordinator: DraftTaskCoordinator,
        private val observeAuthSessionUseCase: ObserveAuthSessionUseCase,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) : DefaultLifecycleObserver {
        private var lifecycleJob: Job? = null

        override fun onStart(owner: LifecycleOwner) {
            lifecycleJob?.cancel()
            lifecycleJob =
                applicationScope.launch {
                    observeAuthSessionUseCase().collect { state ->
                        when (state) {
                            AuthSessionState.Loading -> coordinator.onBackground()
                            AuthSessionState.Authenticated -> coordinator.onForeground()
                            AuthSessionState.Unauthenticated -> coordinator.discard()
                        }
                    }
                }
        }

        override fun onStop(owner: LifecycleOwner) {
            lifecycleJob?.cancel()
            lifecycleJob = applicationScope.launch { coordinator.onBackground() }
        }
    }
