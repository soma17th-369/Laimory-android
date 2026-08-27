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

        /**
         * 계정이 사라지면 값을 모름으로 되돌리고 저장분을 모두 비운다.
         *
         * **아직 못 올린 완료도 함께 버린다.** 남겨 두면 다음에 들어온 계정이 그 표시를 자기
         * 것으로 읽어, 온보딩을 한 번도 하지 않은 사람이 곧장 홈으로 간다. 계정별로 나눠 보관하려면
         * 계정 식별자가 필요한데 앱에는 없다 — [com.soma369.laimory.core.domain.model.auth.SignedInAccount]
         * 는 provider 뿐이고 서버도 subject 를 내려 주지 않는다.
         *
         * 그래서 **오프라인에서 완료한 뒤 한 번도 온라인이 되지 못한 채 로그아웃하면 재시도하지
         * 않는다.** 같은 계정으로 다시 들어오면 서버가 미완료라고 답해 온보딩을 한 번 더 본다.
         * 조회 실패 때와 같은 대가이며, 그 반대(다른 계정에게 남의 완료를 물려주는 것)보다 낫다.
         */
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
