package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.exception.DraftPhotoAccessException
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.timeline.DraftConsentSubmissionGate
import com.soma369.laimory.core.domain.navigation.DraftConsentDetailPage
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
        submissionGate: DraftConsentSubmissionGate,
    ) : BaseMviViewModel<DraftConsentUiState, DraftConsentUiIntent, DraftConsentUiSideEffect>(
            DraftConsentUiState(isSubmissionAllowed = submissionGate.isSubmissionAllowed()),
        ) {
        private val isSubmissionAllowed = submissionGate.isSubmissionAllowed()
        private var activePreparation: DraftConsentPreparation? = null

        init {
            safeLaunch {
                sessionStore.preparation.collect { preparation ->
                    when {
                        // 폐기(null) 시 activity 범위 ViewModel 에 알림 본문·사진 URI 같은
                        // 민감 표시 모델과 체크 상태가 남지 않도록 즉시 초기화한다.
                        preparation == null -> {
                            activePreparation = null
                            updateState { initialUiState() }
                        }

                        preparation.attemptId != activePreparation?.attemptId -> {
                            activePreparation = preparation
                            updateState { initialUiState().copy(content = preparation.toConsentContent()) }
                        }
                    }
                }
            }
        }

        override suspend fun handleIntent(intent: DraftConsentUiIntent) {
            when (intent) {
                is DraftConsentUiIntent.ToggleTerm -> toggleTerm(intent)
                is DraftConsentUiIntent.OpenTypeDetail -> openTypeDetail(intent)
                DraftConsentUiIntent.CloseTypeDetail -> navigationHelper.navigateToBack()
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
            val summary = state.value.content?.summaryOf(intent.group) ?: return
            if (!summary.isSent) return
            navigationHelper.navigateTo(DraftConsentDetailPage(intent.group.name))
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
            // 스냅샷 확정 뒤 사진이 삭제되거나 권한이 바뀐 경우 — 같은 스냅샷 재시도로는 복구되지
            // 않으므로 준비를 폐기하고 홈의 사진 재선택 흐름으로 복귀시킨다.
            if (error is DraftPhotoAccessException) {
                sessionStore.clearPreparation()
                sessionStore.markPhotoReselectionNeeded()
                activePreparation = null
                navigationHelper.navigateToBack()
                return
            }
            // 그 외에는 같은 스냅샷으로 재시도할 수 있게 화면에 머물러 안내한다.
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

        private fun initialUiState(): DraftConsentUiState = DraftConsentUiState(isSubmissionAllowed = isSubmissionAllowed)
    }
