package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.DraftPhotoAccessException
import com.soma369.laimory.core.domain.exception.StaleTermVersionException
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.timeline.LocationMapRenderGate
import com.soma369.laimory.core.domain.navigation.DraftConsentDetailPage
import com.soma369.laimory.core.domain.navigation.DraftLoadingPage
import com.soma369.laimory.core.domain.usecase.CreateTimelineDraftUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.home.draft.DraftConsentPreparation
import com.soma369.laimory.feature.home.draft.DraftConsentSessionStore
import com.soma369.laimory.feature.home.draft.DraftLoadingSessionStore
import com.soma369.laimory.feature.home.draft.toLoadingSession
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
        private val loadingSessionStore: DraftLoadingSessionStore,
        private val createTimelineDraftUseCase: CreateTimelineDraftUseCase,
        private val draftTaskCoordinator: DraftTaskCoordinator,
        private val navigationHelper: NavigationHelper,
        private val termsCoordinator: TermsAgreementCoordinator,
        mapRenderGate: LocationMapRenderGate,
    ) : BaseMviViewModel<DraftConsentUiState, DraftConsentUiIntent, DraftConsentUiSideEffect>(
            DraftConsentUiState(isMapRenderAllowed = mapRenderGate.isMapRenderAllowed()),
        ) {
        private val isMapRenderAllowed = mapRenderGate.isMapRenderAllowed()
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
                            loadTermRequirement()
                        }
                    }
                }
            }
        }

        /**
         * 서버 이력으로 이 계정이 아직 받아야 할 동의를 가른다.
         *
         * 이미 동의한 것은 확인 대상에서 빼고 원문 열람만 남긴다 — 화면에서 해제해도 서버에
         * 철회 API 가 없어 실제로 철회되지 않으므로, 되돌릴 수 없는 것을 되돌릴 수 있게 보이면 안 된다.
         *
         * 조회에 실패하면 요구를 세우지 않는다. 서버가 gate 를 들고 있어 실제로 미동의면 제출이
         * 거절되고 그때 다시 판정한다 — 앱이 여기서 막아 봐야 더 정확해지지 않는다.
         */
        private suspend fun loadTermRequirement() {
            val requirement = termsCoordinator.requirementOf(TermStage.TIMELINE_FIRST_CREATE).getOrNull()
            updateState {
                copy(
                    pendingTerms = requirement?.pending.orEmpty(),
                    agreedTerms = requirement?.items?.filter { it.isAgreed }?.map { it.document }.orEmpty(),
                    checkedTerms = emptySet(),
                )
            }
        }

        override suspend fun handleIntent(intent: DraftConsentUiIntent) {
            when (intent) {
                is DraftConsentUiIntent.ToggleTerm -> toggleTerm(intent)
                is DraftConsentUiIntent.ToggleItemInclusion -> toggleItemInclusion(intent)
                DraftConsentUiIntent.ToggleLocationInclusion -> toggleLocationInclusion()
                is DraftConsentUiIntent.OpenTypeDetail -> openTypeDetail(intent)
                DraftConsentUiIntent.CloseTypeDetail -> navigationHelper.navigateToBack()
                DraftConsentUiIntent.Submit -> submit()
                DraftConsentUiIntent.NavigateBack -> navigateBack()
            }
        }

        private fun toggleTerm(intent: DraftConsentUiIntent.ToggleTerm) {
            if (state.value.isSubmitting) return
            updateState {
                copy(
                    checkedTerms =
                        if (intent.termType in checkedTerms) {
                            checkedTerms - intent.termType
                        } else {
                            checkedTerms + intent.termType
                        },
                )
            }
        }

        private fun toggleItemInclusion(intent: DraftConsentUiIntent.ToggleItemInclusion) {
            if (state.value.isSubmitting) return
            val preparation = activePreparation ?: return
            val item = preparation.selection.items.firstOrNull { it.rawId == intent.itemKey } ?: return
            // 사진은 홈 사진 시트 선택이 정본이므로 여기서 제외할 수 없다.
            if (item.itemType == ItemType.PHOTO) return
            updateState {
                copy(
                    excludedRawIds =
                        if (intent.itemKey in excludedRawIds) {
                            excludedRawIds - intent.itemKey
                        } else {
                            excludedRawIds + intent.itemKey
                        },
                )
            }
        }

        /**
         * 현재 생성 시도의 위치 항목 전체를 한 번에 포함·제외한다.
         *
         * 켤 때 최초 스냅샷의 위치 rawId 만 제외 집합에서 뺀다 — 다른 유형이 제외한 항목을 함께
         * 되살리지 않기 위해서다. 상한 여유 재충원은 [DraftSourceItemSelection.excluding] 정책 그대로
         * 하지 않는다.
         */
        private fun toggleLocationInclusion() {
            if (state.value.isSubmitting) return
            val locationRawIds = state.value.content?.locationRawIds.orEmpty()
            if (locationRawIds.isEmpty()) return
            updateState {
                copy(
                    excludedRawIds =
                        if (isLocationIncluded) excludedRawIds + locationRawIds else excludedRawIds - locationRawIds,
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
            // 스냅샷에서 사용자 제외 항목만 뺀 결과를 전송한다. 제외로 생긴 상한 여유는 재충원하지 않는다.
            val submission = preparation.selection.excluding(state.value.excludedRawIds)
            updateState { copy(isSubmitting = true, submitError = null) }
            safeLaunch(onError = ::handleSubmitFailure) {
                if (!recordAgreements()) return@safeLaunch
                if (preparation.discardActiveTask) draftTaskCoordinator.discard()
                val result =
                    createTimelineDraftUseCase(
                        preparation.recordDate,
                        preparation.zone,
                        preparation.window,
                        submission,
                    )
                val handle =
                    result.getOrElse {
                        handleSubmitFailure(it)
                        return@safeLaunch
                    }
                draftTaskCoordinator.start(handle.taskId, preparation.recordDate)
                // 준비 상태는 여기서 폐기되므로, 로딩 화면이 쓸 것만 먼저 옮겨 담는다.
                loadingSessionStore.start(submission.toLoadingSession(handle.taskId, preparation.recordDate))
                sessionStore.clearPreparation()
                activePreparation = null
                // 동의 화면을 백스택에서 빼고 로딩 화면을 올린다 — 로딩에서 뒤로가면 홈이다.
                navigationHelper.navigateToBack()
                navigationHelper.navigateTo(DraftLoadingPage)
            }
        }

        /**
         * 남은 필수 동의를 먼저 기록한다. 실패하면 초안 생성으로 넘어가지 않는다.
         *
         * 개정 경쟁이면 새 버전으로 자동 재시도하지 않는다 — 사용자가 읽지 않은 내용에 동의한
         * 기록이 서버에 남는다. 다시 조회해 바뀐 문서를 싣고 확인 상태를 되돌린다.
         *
         * **성공한 동의는 초안 생성이 실패해도 되돌리지 않는다.** 같은 스냅샷으로 다시 제출할 때
         * 이미 기록된 동의는 확인 대상에서 빠진다.
         */
        private suspend fun recordAgreements(): Boolean {
            val pending = state.value.pendingTerms
            if (pending.isEmpty()) return true

            val error = termsCoordinator.agree(pending).exceptionOrNull()
            if (error == null) {
                // 기록된 동의를 화면에도 반영한다. 초안 생성이 실패해 다시 제출하더라도 이미
                // 기록된 것을 또 확인하게 하지 않는다.
                loadTermRequirement()
                return true
            }

            if (error is StaleTermVersionException) {
                loadTermRequirement()
                updateState { copy(isSubmitting = false, submitError = REVISED_MESSAGE) }
            } else {
                updateState { copy(isSubmitting = false, submitError = AGREEMENT_FAILURE_MESSAGE) }
            }
            return false
        }

        private fun handleSubmitFailure(error: Throwable) {
            // 서버가 이 단계 동의를 다시 요구한다 — 다른 기기에서 개정본이 적용됐거나 판정이
            // 어긋난 경우다. 서버 이력을 다시 읽어 확인 항목을 되살린다. 같은 스냅샷으로 다시
            // 제출할 수 있으므로 준비는 폐기하지 않는다.
            if (error is ApiException && error.errorCode == TERMS_AGREEMENT_REQUIRED) {
                safeLaunch(onError = { }) { loadTermRequirement() }
                updateState { copy(isSubmitting = false, submitError = AGREEMENT_REQUIRED_MESSAGE) }
                return
            }
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

        private fun initialUiState(): DraftConsentUiState = DraftConsentUiState(isMapRenderAllowed = isMapRenderAllowed)

        private companion object {
            /** 서버가 이 단계 동의를 요구할 때 주는 코드. */
            const val TERMS_AGREEMENT_REQUIRED = -3001

            const val REVISED_MESSAGE = "약관이 개정돼 다시 확인이 필요해요."
            const val AGREEMENT_FAILURE_MESSAGE = "동의를 기록하지 못했어요. 잠시 후 다시 시도해주세요."
            const val AGREEMENT_REQUIRED_MESSAGE = "동의가 다시 필요해요. 아래 항목을 확인해주세요."
        }
    }
