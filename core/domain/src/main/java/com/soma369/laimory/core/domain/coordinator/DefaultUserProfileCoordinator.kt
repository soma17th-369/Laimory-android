package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.user.UserProfile
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import com.soma369.laimory.core.domain.usecase.user.GetUserProfileUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class DefaultUserProfileCoordinator
    @Inject
    constructor(
        private val getUserProfileUseCase: GetUserProfileUseCase,
        observeSignedInAccountUseCase: ObserveSignedInAccountUseCase,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) : UserProfileCoordinator {
        private val mutex = Mutex()
        private val mutableProfile = MutableStateFlow<UserProfile?>(null)
        private var fetchJob: Job? = null

        /**
         * 지금 유효한 세션의 일련번호.
         *
         * 로그아웃할 때마다 올린다. 응답이 늦게 도착한 이전 세션의 프로필이 새 세션 상태를 덮어쓰면
         * 계정을 바꿨는데 이전 계정 닉네임이 뜨므로, 결과를 반영하기 전에 번호를 확인한다.
         */
        private var sessionEpoch = 0L

        override val profile: StateFlow<UserProfile?> = mutableProfile.asStateFlow()

        init {
            applicationScope.launch {
                // 세션 전이를 정본으로 삼는다. Root 교체는 화면 ViewModel 재생성을 보장하지 않아
                // 재로그인해도 이전 계정 닉네임이 남을 수 있다.
                observeSignedInAccountUseCase().collect { account ->
                    if (account == null) clearSession() else fetchIfNeeded()
                }
            }
        }

        override fun refresh() {
            applicationScope.launch { fetchIfNeeded() }
        }

        private suspend fun clearSession() =
            mutex.withLock {
                sessionEpoch++
                fetchJob?.cancel()
                fetchJob = null
                mutableProfile.value = null
            }

        private suspend fun fetchIfNeeded() {
            mutex.withLock {
                // 같은 세션에서 이미 성공했으면 다시 묻지 않는다.
                if (mutableProfile.value != null) return@withLock
                // 홈과 설정이 동시에 요구해도 요청은 하나다.
                if (fetchJob?.isActive == true) return@withLock
                val epoch = sessionEpoch
                fetchJob = applicationScope.launch { fetchInto(epoch) }
            }
        }

        private suspend fun fetchInto(epoch: Long) {
            val result = getUserProfileUseCase()
            mutex.withLock {
                // 조회 도중 로그아웃했으면 결과를 버린다.
                if (epoch != sessionEpoch) return@withLock
                // 실패는 조용히 둔다 — 화면이 fallback 문구를 쓰고, refresh 로 다시 시도할 수 있다.
                result.getOrNull()?.let { mutableProfile.value = it }
            }
        }
    }
