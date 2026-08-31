package com.soma369.laimory.feature.onboarding.viewmodel

import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.exception.StaleTermVersionException
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.usecase.CompleteOnboardingUseCase
import com.soma369.laimory.core.domain.usecase.ObserveOnboardingProgressUseCase
import com.soma369.laimory.core.domain.usecase.SaveOnboardingProgressUseCase
import com.soma369.laimory.core.domain.usecase.SetLocationTrackingUseCase
import com.soma369.laimory.core.domain.usecase.terms.GetDisplayTermsUseCase
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
        private val getDisplayTerms: GetDisplayTermsUseCase,
    ) : BaseMviViewModel<OnboardingUiState, OnboardingUiIntent, OnboardingUiSideEffect>(OnboardingUiState()) {
        /**
         * 실제로 서버에 보낼 문서. 화면에 보이는 목록과 다를 수 있다.
         *
         * 이 환경 catalog 가 비어 있으면 화면은 게시된 정본을 보여 주지만 등록 대상은 없다 —
         * 그 환경 DB 에 없는 행을 보내면 전부 거절되고, 서버도 그 단계를 강제하지 않는다.
         */
        private var recordableConsents: List<TermDocument> = emptyList()

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
         * 마지막 장에서 받을 필수 동의를 모은다.
         *
         * 위치약관까지 네 종류를 한 자리에서 받는다. 서버는 위치정보가 실린 요청에만 위치약관을
         * 요구하지만, 온보딩 시점에는 앞으로 무엇을 보낼지 알 수 없어 그 조건을 판정할 수 없다.
         * 미리 받아 두면 초안 생성 화면이 다시 묻지 않는다.
         *
         * **보여 줄 목록과 보낼 목록을 나눈다.** 이 환경 catalog 가 통째로 비어 있으면 화면은
         * 게시된 정본을 보여 주되 등록 대상은 비운다 — 그 환경 DB 에 없는 행을 보내면 전부
         * 거절되고, 서버도 catalog 가 없는 단계는 강제하지 않는다(fail-open).
         *
         * 문서가 하나라도 있는 환경에서는 그 판정이 정본이다. 이미 다 동의했으면 목록이 비고
         * 체크리스트도 그리지 않는다. 조회 실패도 같다 — 여기서 막아 온보딩을 못 끝내게 할
         * 이유가 없고, 동의는 초안 생성 화면이 다시 받는다.
         */
        private suspend fun prepareConsentPage() {
            val requirements = CONSENT_STAGES.mapNotNull { termsCoordinator.requirementOf(it).getOrNull() }
            val pending = requirements.flatMap { it.pending }.distinctBy { it.termType }
            recordableConsents = pending

            // 이 환경에 문서가 하나라도 있으면 그 판정이 정본이다. 이미 다 동의했으면 목록이 비고
            // 체크리스트도 그리지 않는다.
            val hasCatalog = requirements.any { it.items.isNotEmpty() }
            val display = if (hasCatalog) pending else getDisplayTerms(CONSENT_TYPES)
            updateState { copy(consentDocuments = display) }
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
         * 남은 필수 동의를 기록한다. 실패하면 온보딩을 끝내지 않는다.
         *
         * 개정 경쟁이면 새 버전으로 자동 재시도하지 않는다 — 사용자가 열람하지 않은 내용에
         * 동의한 기록이 서버에 남는다. 다시 조회해 바뀐 문서를 싣고 확인 상태를 모두 되돌린다.
         */
        private suspend fun recordConsents(): Boolean {
            val documents = recordableConsents
            if (documents.isEmpty()) return true

            val error = termsCoordinator.agree(documents).exceptionOrNull() ?: return true
            if (error is StaleTermVersionException) {
                prepareConsentPage()
                updateState { copy(checkedConsents = emptySet(), consentErrorMessage = REVISED_MESSAGE) }
            } else {
                updateState { copy(consentErrorMessage = FAILURE_MESSAGE) }
            }
            return false
        }

        /**
         * 필수 동의를 기록한 뒤 완료를 저장한다.
         *
         * 화면 전환을 여기서 직접 하지 않는다 — 저장이 반영되면 앱 루트가 스스로 Home 으로
         * 바뀐다. 저장 전에 넘기면 그 사이 앱이 죽었을 때 다음 실행에서 온보딩을 처음부터 다시 본다.
         */
        private suspend fun complete() {
            updateState { copy(isCompleting = true, hasCompletionFailed = false, consentErrorMessage = null) }
            if (!recordConsents()) {
                updateState { copy(isCompleting = false) }
                return
            }
            markCompleted()
        }

        /** 동의를 건드리지 않고 완료만 저장한다. */
        private suspend fun markCompleted() {
            updateState { copy(isCompleting = true, hasCompletionFailed = false) }
            runCatching { completeOnboardingUseCase() }
                .onFailure { updateState { copy(isCompleting = false, hasCompletionFailed = true) } }
        }

        private companion object {
            /** 마지막 장에서 한 번에 받는 필수 동의. 서버 단계 정의를 그대로 쓴다. */
            val CONSENT_STAGES = listOf(TermStage.TIMELINE_FIRST_CREATE, TermStage.TIMELINE_LOCATION)

            val CONSENT_TYPES = CONSENT_STAGES.flatMap { it.requiredTypes }

            const val REVISED_MESSAGE = "약관이 개정돼 다시 확인이 필요해요."
            const val FAILURE_MESSAGE = "동의를 기록하지 못했어요. 잠시 후 다시 시도해 주세요."
        }
    }
