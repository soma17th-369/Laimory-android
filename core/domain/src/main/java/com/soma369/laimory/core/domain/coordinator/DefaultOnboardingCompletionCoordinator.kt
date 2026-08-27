package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.repository.OnboardingRepository
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class DefaultOnboardingCompletionCoordinator
    @Inject
    constructor(
        private val repository: OnboardingRepository,
        observeSignedInAccountUseCase: ObserveSignedInAccountUseCase,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) : OnboardingCompletionCoordinator {
        private val mutex = Mutex()
        private val mutableCompleted = MutableStateFlow<Boolean?>(null)
        private var fetchJob: Job? = null

        /**
         * 지금 유효한 세션의 일련번호.
         *
         * 로그아웃할 때마다 올린다. 늦게 도착한 이전 세션의 응답이 새 세션 값을 덮어쓰면 계정을
         * 바꿨는데 이전 계정의 완료가 적용되므로, 반영 전에 번호를 확인한다.
         */
        private var sessionEpoch = 0L

        override val completed: StateFlow<Boolean?> = mutableCompleted.asStateFlow()

        init {
            applicationScope.launch {
                observeSignedInAccountUseCase().collect { account ->
                    if (account == null) clearSession() else fetchIfNeeded()
                }
            }
        }

        override fun refresh() {
            applicationScope.launch { fetchIfNeeded() }
        }

        /**
         * 완료를 확정한다.
         *
         * 서버 응답을 기다리지 않고 값을 먼저 올린다 — 기록이 늦다고 사용자를 온보딩에 묶어 둘
         * 이유가 없다. 대신 **올리기 전에 대기 표시를 남긴다.** 완료 여부의 정본이 서버라,
         * 표시 없이 실패하면 다음 실행에서 서버가 `false` 를 주고 끝낸 온보딩을 다시 본다.
         */
        override suspend fun markCompleted() {
            repository.setCompletionPending(true)
            repository.cacheCompletion(true)
            mutex.withLock { mutableCompleted.value = true }
            syncPendingCompletion()
        }

        override suspend fun resetForCurrentSession() {
            mutex.withLock {
                fetchJob?.cancel()
                fetchJob = null
                mutableCompleted.value = false
            }
            repository.clear()
        }

        /** 계정이 사라지면 값을 모름으로 되돌리고 캐시도 비운다 — 다음 계정이 깨끗하게 판정한다. */
        private suspend fun clearSession() {
            mutex.withLock {
                sessionEpoch++
                fetchJob?.cancel()
                fetchJob = null
                mutableCompleted.value = null
            }
            repository.clear()
        }

        private suspend fun fetchIfNeeded() {
            mutex.withLock {
                if (mutableCompleted.value != null) return@withLock
                if (fetchJob?.isActive == true) return@withLock
                val epoch = sessionEpoch
                fetchJob = applicationScope.launch { fetchInto(epoch) }
            }
        }

        /** 밀린 완료 기록을 올린다. 멱등이라 여러 번 불러도 안전하고, 성공해야 표시를 지운다. */
        private suspend fun syncPendingCompletion() {
            if (!repository.isCompletionPending()) return
            runCatching { repository.recordCompletion() }
                .onSuccess { repository.setCompletionPending(false) }
        }

        /**
         * 서버 값을 받아 반영한다.
         *
         * 실패하면 캐시로 떨어지고, 캐시도 없으면 **아직 안 함**으로 본다. 조회가 안 된다고 앱을
         * 로딩에 묶어 두면 오프라인 사용자가 들어오지 못한다 — 최악이 온보딩을 한 번 더 보는
         * 것이라면 그쪽이 낫다. 완료 기록은 멱등이라 다시 눌러도 안전하다.
         */
        private suspend fun fetchInto(epoch: Long) {
            syncPendingCompletion()
            // 아직도 못 올렸으면 서버에 묻지 않는다. 서버는 기록을 못 받았으니 `false` 를 줄
            // 텐데, 끝낸 사람에게 온보딩을 다시 보이는 것보다 로컬을 믿는 편이 맞다.
            if (repository.isCompletionPending()) {
                mutex.withLock {
                    if (epoch != sessionEpoch) return@withLock
                    mutableCompleted.value = true
                }
                return
            }
            val result = repository.fetchCompletion()
            result.getOrNull()?.let { repository.cacheCompletion(it) }
            val resolved = result.getOrNull() ?: repository.cachedCompletion() ?: false
            mutex.withLock {
                if (epoch != sessionEpoch) return@withLock
                mutableCompleted.value = resolved
            }
        }
    }
