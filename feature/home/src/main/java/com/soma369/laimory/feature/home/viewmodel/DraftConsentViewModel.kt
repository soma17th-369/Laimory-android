package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.usecase.CreateTimelineDraftUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.home.draft.DraftConsentPreparation
import com.soma369.laimory.feature.home.draft.DraftConsentSessionStore
import com.soma369.laimory.feature.home.state.DraftConsentUiIntent
import com.soma369.laimory.feature.home.state.DraftConsentUiSideEffect
import com.soma369.laimory.feature.home.state.DraftConsentUiState
import com.soma369.laimory.feature.home.state.toConsentContent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 데이터 전송 확인·동의 화면의 ViewModel.
 *
 * 홈이 확정한 생성 시도 스냅샷([DraftConsentSessionStore])을 구독해 화면을 구성하고,
 * 필수 동의 완료 후에만 스냅샷 그대로 제출한다. 새 attemptId 가 들어올 때마다
 * 체크·오류 상태를 초기화하므로 재진입은 항상 새 생성 시도로 시작한다.
 */
@HiltViewModel
class DraftConsentViewModel
    @Inject
    constructor(
        private val sessionStore: DraftConsentSessionStore,
        private val createTimelineDraftUseCase: CreateTimelineDraftUseCase,
        private val draftTaskCoordinator: DraftTaskCoordinator,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<DraftConsentUiState, DraftConsentUiIntent, DraftConsentUiSideEffect>(
            DraftConsentUiState(),
        ) {
        private var activePreparation: DraftConsentPreparation? = null

        init {
            safeLaunch {
                sessionStore.preparation.collect { preparation ->
                    // 폐기(null)는 화면 이탈 경로에서만 발생하므로 상태를 건드리지 않는다.
                    // 준비물 없이 화면이 복원된 경우(프로세스 재생성)는 초기 상태(content=null)가 안내를 담당한다.
                    if (preparation != null && preparation.attemptId != activePreparation?.attemptId) {
                        activePreparation = preparation
                        updateState { DraftConsentUiState(content = preparation.toConsentContent()) }
                    }
                }
            }
        }

        override suspend fun handleIntent(intent: DraftConsentUiIntent) {
            when (intent) {
                is DraftConsentUiIntent.ToggleTerm -> toggleTerm(intent)
                is DraftConsentUiIntent.OpenTypeDetail -> openTypeDetail(intent)
                DraftConsentUiIntent.CloseTypeDetail -> updateState { copy(openTypeDetail = null) }
                is DraftConsentUiIntent.OpenTermsDetail -> updateState { copy(openTermsDetail = intent.term) }
                DraftConsentUiIntent.CloseTermsDetail -> updateState { copy(openTermsDetail = null) }
                DraftConsentUiIntent.Submit -> submit()
                DraftConsentUiIntent.NavigateBack -> navigateBack()
            }
        }

        private fun toggleTerm(intent: DraftConsentUiIntent.ToggleTerm) {
            if (state.value.isSubmitting) return
            updateState {
                copy(
                    checkedTerms =
                        if (intent.term in checkedTerms) checkedTerms - intent.term else checkedTerms + intent.term,
                )
            }
        }

        private fun openTypeDetail(intent: DraftConsentUiIntent.OpenTypeDetail) {
            val summary = state.value.content?.typeSummaries?.firstOrNull { it.group == intent.group } ?: return
            if (!summary.isSent) return
            updateState { copy(openTypeDetail = intent.group) }
        }

        private fun submit() {
            val preparation = activePreparation ?: return
            if (!state.value.canSubmit) return
            updateState { copy(isSubmitting = true, submitError = null) }
            safeLaunch(onError = ::handleSubmitFailure) {
                if (preparation.discardActiveTask) draftTaskCoordinator.discard()
                val result =
                    createTimelineDraftUseCase(
                        preparation.recordDate,
                        preparation.zone,
                        preparation.window,
                        preparation.selection,
                    )
                val handle =
                    result.getOrElse {
                        handleSubmitFailure(it)
                        return@safeLaunch
                    }
                draftTaskCoordinator.start(handle.taskId, preparation.recordDate)
                sessionStore.clearPreparation()
                sessionStore.markSubmitted()
                activePreparation = null
                navigationHelper.navigateToBack()
            }
        }

        private fun handleSubmitFailure(error: Throwable) {
            // 같은 스냅샷으로 재시도 가능한 오류는 화면에 머물러 안내한다.
            // 입력을 다시 골라야 하는 오류(접근 불가 사진 등)는 스냅샷 확정 단계에서 이미 홈이 걸러낸다.
            updateState {
                copy(
                    isSubmitting = false,
                    submitError = "초안 생성 요청을 보내지 못했어요. 잠시 후 다시 시도해주세요.",
                )
            }
            handleFailure(error)
        }

        private fun navigateBack() {
            if (state.value.isSubmitting) return
            sessionStore.clearPreparation()
            activePreparation = null
            navigationHelper.navigateToBack()
        }
    }
