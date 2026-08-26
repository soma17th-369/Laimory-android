package com.soma369.laimory.feature.onboarding.viewmodel

import com.soma369.laimory.core.domain.usecase.CompleteOnboardingUseCase
import com.soma369.laimory.core.domain.usecase.ObserveOnboardingStateUseCase
import com.soma369.laimory.core.domain.usecase.SaveOnboardingProgressUseCase
import com.soma369.laimory.core.domain.usecase.user.ObserveUserProfileUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.onboarding.model.indexOfKeyOrFirst
import com.soma369.laimory.feature.onboarding.state.OnboardingUiIntent
import com.soma369.laimory.feature.onboarding.state.OnboardingUiSideEffect
import com.soma369.laimory.feature.onboarding.state.OnboardingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val observeOnboardingStateUseCase: ObserveOnboardingStateUseCase,
        private val observeUserProfileUseCase: ObserveUserProfileUseCase,
        private val saveOnboardingProgressUseCase: SaveOnboardingProgressUseCase,
        private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    ) : BaseMviViewModel<OnboardingUiState, OnboardingUiIntent, OnboardingUiSideEffect>(OnboardingUiState()) {
        init {
            restoreLastPage()
            observeNickname()
        }

        /**
         * 마지막으로 본 장을 **한 번만** 읽는다.
         *
         * 계속 관찰하면 사용자가 장을 넘길 때마다 저장 → 방출 → 복원이 되돌아와 Pager 가 제자리에
         * 묶인다. 복원은 화면에 들어올 때 한 번이면 된다.
         */
        private fun restoreLastPage() {
            safeLaunch {
                val saved = observeOnboardingStateUseCase().first()
                val index = state.value.pages.indexOfKeyOrFirst(saved.lastPageKey)
                updateState { copy(initialPageIndex = index) }
                sendEffect(OnboardingUiSideEffect.RestorePage(index))
            }
        }

        /** 닉네임은 있으면 인사말에 쓰고, 없거나 조회가 실패해도 온보딩을 막지 않는다. */
        private fun observeNickname() {
            safeLaunch(onError = { }) {
                observeUserProfileUseCase().collect { profile ->
                    updateState { copy(nickname = profile?.nickname) }
                }
            }
        }

        override suspend fun handleIntent(intent: OnboardingUiIntent) {
            when (intent) {
                is OnboardingUiIntent.PageChanged -> saveProgress(intent.pageIndex)
                OnboardingUiIntent.Complete -> complete()
            }
        }

        /** 진행 기록 실패는 알리지 않는다 — 다음에 첫 장부터 볼 뿐 지금 흐름을 막을 이유가 없다. */
        private fun saveProgress(pageIndex: Int) {
            val key = state.value.pages.getOrNull(pageIndex)?.key ?: return
            safeLaunch(onError = { }) { saveOnboardingProgressUseCase(key) }
        }

        /**
         * 완료를 저장한 뒤에야 Home 으로 간다.
         *
         * 화면 전환을 여기서 직접 하지 않는다 — 저장이 반영되면 앱 루트가 스스로 Home 으로
         * 바뀐다. 저장 전에 넘기면 그 사이 앱이 죽었을 때 다음 실행에서 온보딩을 처음부터 다시 본다.
         */
        private suspend fun complete() {
            updateState { copy(isCompleting = true, hasCompletionFailed = false) }
            runCatching { completeOnboardingUseCase() }
                .onFailure { updateState { copy(isCompleting = false, hasCompletionFailed = true) } }
        }
    }
