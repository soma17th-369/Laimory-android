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
import kotlinx.coroutines.delay
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
            // 복원을 약관 조회 뒤로 미루지 않는다. 장 목록은 조회 결과와 무관하게 고정이고,
            // 조회는 네트워크를 두 번 탈 수 있어 그동안 화면이 첫 장에 묶인다.
            safeLaunch(onError = { }) { restoreLastPage() }
            safeLaunch(onError = { }) { prepareConsentPage() }
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
            val required = requirements.flatMap { it.items }.distinctBy { it.document.termType }
            val display = if (hasCatalog) required.map { it.document } else getDisplayTerms(CONSENT_TYPES)
            // 이미 동의한 항목은 목록에서 빼지 않고 체크된 채로 잠근다. 빼 버리면 다시 온
            // 사용자에게는 목록이 통째로 사라져, 무엇에 동의하고 시작하는지 알 수 없다.
            val locked = required.filter { it.isAgreed }.map { it.document.termType }.toSet()
            updateState {
                copy(
                    consentDocuments = display,
                    lockedConsents = locked,
                    checkedConsents = checkedConsents + locked,
                )
            }
        }

        /**
         * 마지막으로 본 장을 **한 번만** 읽는다.
         *
         * 계속 관찰하면 사용자가 장을 넘길 때마다 저장 → 방출 → 복원이 되돌아와 Pager 가 제자리에
         * 묶인다. 복원은 화면에 들어올 때 한 번이면 된다.
         *
         * 값을 상태로만 준다. 효과로 밀어 스크롤시키면 Pager 는 이미 첫 장으로 만들어진 뒤라,
         * 복원이 늦거나 컴포지션이 다시 만들어지는 사이에 첫 장이 보였다 튄다.
         */
        private suspend fun restoreLastPage() {
            val savedPageKey = observeOnboardingProgressUseCase().first()
            updateState { copy(initialPageIndex = pages.indexOfKeyOrFirst(savedPageKey)) }
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
                is OnboardingUiIntent.PageChanged -> onPageChanged(intent.pageIndex)
                is OnboardingUiIntent.ConsentToggled -> toggleConsent(intent.termType)
                OnboardingUiIntent.Complete -> complete()
                OnboardingUiIntent.EnableLocationTracking -> enableLocationTracking()
            }
        }

        /**
         * 장을 넘길 때마다 복원 인덱스를 함께 올린다.
         *
         * 앱 시작 시점 값으로 굳혀 두면, 원문을 보러 나갔다 오는 사이에 컴포지션이 다시 만들어질 때
         * Pager 가 그 오래된 자리에서 새로 만들어져 첫 장으로 돌아간다. 저장은 다음 실행을 위한
         * 것이고, 이 값은 지금 화면을 위한 것이다.
         */
        private fun onPageChanged(pageIndex: Int) {
            updateState { copy(initialPageIndex = pageIndex) }
            saveProgress(pageIndex)
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

        /** 항목을 직접 켜고 끌 수도 있다. 버튼은 남은 것을 마저 채우는 지름길이다. */
        private fun toggleConsent(termType: TermType) {
            updateState {
                // 이미 기록된 동의는 앱이 되돌릴 수 없다. 끄는 시늉만 하면 다음 조회에서 도로 켜진다.
                if (termType in lockedConsents) return@updateState this
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
                updateState { copy(checkedConsents = lockedConsents, consentErrorMessage = REVISED_MESSAGE) }
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
            // 누른 즉시 잠근다. 연출을 먼저 하면 그 사이 버튼이 살아 있어 두 번 눌린다.
            updateState { copy(isCompleting = true, hasCompletionFailed = false, consentErrorMessage = null) }

            val documents = state.value.consentDocuments
            // 이미 다 동의한 사용자에게는 채울 체크가 없다. 그때도 기다리면 화면은 그대로인 채
            // 버튼만 잠시 먹통이 된다.
            if (recordableConsents.isNotEmpty()) {
                // 무엇에 동의하고 넘어가는지 눈으로 확인할 틈을 준다. 버튼 문구가 `모두 동의하고
                // 시작하기` 라 결과는 이미 분명하지만, 체크가 차오르는 것을 보지 못하면 무엇이
                // 일어났는지 모른 채 화면이 바뀐다.
                updateState { copy(checkedConsents = documents.mapTo(mutableSetOf()) { it.termType }) }
                delay(CONSENT_REVEAL_MILLIS)
            }

            if (!recordConsents()) {
                updateState { copy(isCompleting = false, checkedConsents = lockedConsents) }
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

            /** 체크가 차오르는 것을 보여 주는 시간. 넘기기 전에 한 박자만 둔다. */
            const val CONSENT_REVEAL_MILLIS = 400L

            const val REVISED_MESSAGE = "약관이 개정돼 다시 확인이 필요해요."
            const val FAILURE_MESSAGE = "동의를 기록하지 못했어요. 잠시 후 다시 시도해 주세요."
        }
    }
