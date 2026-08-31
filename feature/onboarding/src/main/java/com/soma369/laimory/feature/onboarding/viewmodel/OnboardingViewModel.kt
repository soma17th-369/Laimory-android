package com.soma369.laimory.feature.onboarding.viewmodel

import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.exception.StaleTermVersionException
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.usecase.CompleteOnboardingUseCase
import com.soma369.laimory.core.domain.usecase.ObserveOnboardingProgressUseCase
import com.soma369.laimory.core.domain.usecase.SaveOnboardingProgressUseCase
import com.soma369.laimory.core.domain.usecase.SetLocationTrackingUseCase
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
        private val observeOnboardingProgressUseCase: ObserveOnboardingProgressUseCase,
        private val observeUserProfileUseCase: ObserveUserProfileUseCase,
        private val saveOnboardingProgressUseCase: SaveOnboardingProgressUseCase,
        private val completeOnboardingUseCase: CompleteOnboardingUseCase,
        private val setLocationTrackingUseCase: SetLocationTrackingUseCase,
        private val termsCoordinator: TermsAgreementCoordinator,
    ) : BaseMviViewModel<OnboardingUiState, OnboardingUiIntent, OnboardingUiSideEffect>(OnboardingUiState()) {
        init {
            // 동의 장이 목록에 남는지 먼저 정한다. 목록이 바뀐 뒤에 복원해야 저장해 둔 장 키가
            // 가리키는 자리가 어긋나지 않는다.
            safeLaunch(onError = { }) {
                prepareConsentPage()
                restoreLastPage()
            }
            observeNickname()
        }

        /**
         * 동의 장에 실제로 받을 것이 있는지 확인한다.
         *
         * 이미 동의했거나 서버 catalog 가 아직 활성화되지 않았으면 장을 통째로 뺀다 — 확인할
         * 것이 없는 장은 무엇을 하라는 화면인지 알 수 없다. 조회에 실패해도 뺀다. 동의는 초안
         * 생성 화면이 다시 받으므로, 여기서 막아 온보딩을 못 끝내게 할 이유가 없다.
         */
        private suspend fun prepareConsentPage() {
            val pending =
                termsCoordinator
                    .requirementOf(TermStage.TIMELINE_FIRST_CREATE)
                    .getOrNull()
                    ?.pending
                    .orEmpty()
            updateState {
                if (pending.isEmpty()) {
                    copy(pages = pages.filter { it.consentStage == null })
                } else {
                    copy(consentDocuments = pending)
                }
            }
        }

        /**
         * 마지막으로 본 장을 **한 번만** 읽는다.
         *
         * 계속 관찰하면 사용자가 장을 넘길 때마다 저장 → 방출 → 복원이 되돌아와 Pager 가 제자리에
         * 묶인다. 복원은 화면에 들어올 때 한 번이면 된다.
         */
        private suspend fun restoreLastPage() {
            val savedPageKey = observeOnboardingProgressUseCase().first()
            val index = state.value.pages.indexOfKeyOrFirst(savedPageKey)
            updateState { copy(initialPageIndex = index) }
            sendEffect(OnboardingUiSideEffect.RestorePage(index))
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
                OnboardingUiIntent.EnableLocationTracking -> enableLocationTracking()
                is OnboardingUiIntent.ConsentToggled -> toggleConsent(intent.termType)
                OnboardingUiIntent.SubmitConsent -> submitConsent()
            }
        }

        /** 진행 기록 실패는 알리지 않는다 — 다음에 첫 장부터 볼 뿐 지금 흐름을 막을 이유가 없다. */
        private fun saveProgress(pageIndex: Int) {
            val key = state.value.pages.getOrNull(pageIndex)?.key ?: return
            safeLaunch(onError = { }) { saveOnboardingProgressUseCase(key) }
        }

        /**
         * 백그라운드 위치가 허용되면 자동 수집을 켠다.
         *
         * 수집 실험실 토글과 **같은 UseCase** 를 쓴다 — 갈라지면 한쪽에서 켠 추적이 다른 쪽에는
         * 꺼진 것으로 보인다. 실패는 알리지 않는다. 권한은 이미 받았고, 사용자가 다시 할 수 있는
         * 일이 없는 배경 작업이다.
         */
        private fun enableLocationTracking() {
            safeLaunch(onError = { }) { setLocationTrackingUseCase(true) }
        }

        private fun toggleConsent(termType: TermType) {
            updateState {
                val next = if (termType in checkedConsents) checkedConsents - termType else checkedConsents + termType
                copy(checkedConsents = next, consentErrorMessage = null)
            }
        }

        /**
         * 동의를 등록한다. 성공해야 다음 장으로 넘어간다.
         *
         * 개정 경쟁이면 새 버전으로 자동 재시도하지 않는다 — 사용자가 읽지 않은 내용에 동의한
         * 기록이 서버에 남는다. 다시 조회해 바뀐 문서를 싣고 확인 상태를 모두 되돌린다.
         */
        private suspend fun submitConsent() {
            val documents = state.value.consentDocuments
            if (!state.value.canSubmitConsent) return
            updateState { copy(isConsentSubmitting = true, consentErrorMessage = null) }

            val error = termsCoordinator.agree(documents).exceptionOrNull()
            if (error == null) {
                updateState { copy(isConsentSubmitting = false, consentErrorMessage = null) }
                sendEffect(OnboardingUiSideEffect.ConsentAccepted)
                return
            }
            if (error is StaleTermVersionException) {
                val revised =
                    termsCoordinator
                        .requirementOf(TermStage.TIMELINE_FIRST_CREATE)
                        .getOrNull()
                        ?.pending
                        .orEmpty()
                updateState {
                    copy(
                        isConsentSubmitting = false,
                        consentDocuments = revised.ifEmpty { consentDocuments },
                        checkedConsents = emptySet(),
                        consentErrorMessage = REVISED_MESSAGE,
                    )
                }
                return
            }
            updateState { copy(isConsentSubmitting = false, consentErrorMessage = FAILURE_MESSAGE) }
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

        private companion object {
            const val REVISED_MESSAGE = "약관이 개정돼 다시 확인이 필요해요."
            const val FAILURE_MESSAGE = "동의를 기록하지 못했어요. 잠시 후 다시 시도해 주세요."
        }
    }
