package com.soma369.laimory.feature.terms.viewmodel

import androidx.lifecycle.viewModelScope
import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.exception.StaleTermVersionException
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import com.soma369.laimory.core.domain.usecase.auth.LogoutUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.terms.state.TermsUiIntent
import com.soma369.laimory.feature.terms.state.TermsUiSideEffect
import com.soma369.laimory.feature.terms.state.TermsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TermsViewModel
    @Inject
    constructor(
        private val coordinator: TermsAgreementCoordinator,
        private val logout: LogoutUseCase,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<TermsUiState, TermsUiIntent, TermsUiSideEffect>(TermsUiState()) {
        /** 단계 동의 모드로 열릴 때 받은 후보 단계. 개정으로 다시 조회할 때 같은 집합을 쓴다. */
        private var stageCandidates: List<TermStage> = emptyList()

        init {
            viewModelScope.launch {
                coordinator.loginGate.collect { gate ->
                    updateState {
                        copy(
                            gate = gate,
                            termsOfService =
                                (gate as? TermsGateState.Required)
                                    ?.documents
                                    ?.firstOrNull { it.termType == TermType.TERMS_OF_SERVICE },
                        )
                    }
                    // 처리방침은 동의 대상이 아니지만 인접 위치에 함께 안내해야 한다.
                    if (gate is TermsGateState.Required) loadPrivacyPolicy()
                }
            }
        }

        override suspend fun handleIntent(intent: TermsUiIntent) {
            when (intent) {
                TermsUiIntent.InitializeLogin -> initializeLogin()
                is TermsUiIntent.InitializeStages -> initializeStages(intent.stages)
                is TermsUiIntent.StageTermToggled -> toggleStageTerm(intent.termType)
                TermsUiIntent.AgreeClicked -> agree()
                TermsUiIntent.RetryClicked -> retry()
                TermsUiIntent.LogoutClicked -> signOut()
            }
        }

        /**
         * 로그인 단계 화면으로 되돌린다.
         *
         * 이 ViewModel 은 Activity 범위라 단계 화면과 인스턴스를 공유한다. 여기서 비우지 않으면
         * 단계 동의를 한 번 연 계정이 로그아웃한 뒤에도 초안 동의 화면을 보게 되고, 버튼이
         * 단계 동의를 등록하려 들어 이용약관을 끝낼 수 없다.
         */
        private suspend fun initializeLogin() {
            stageCandidates = emptyList()
            updateState {
                copy(
                    isStageMode = false,
                    stageDocuments = emptyList(),
                    checkedStageTerms = emptySet(),
                    isSubmitting = false,
                    errorMessage = null,
                )
            }
        }

        /**
         * 단계 동의 모드로 연다.
         *
         * 어느 단계가 비었는지는 `-3001` 이 알려 주지 않으므로 후보 단계를 **모두** 다시 조회해
         * 아직 동의가 없는 문서만 모은다. 한 단계씩 순차로 받지 않는다 — 화면이 두 번 뜨고,
         * catalog 에 없는 종류는 조회에서 빠져 단계가 열리므로 순서를 따질 실익이 없다.
         *
         * 조회에 실패하거나 받을 것이 없으면 화면을 닫는다. 빈 목록을 띄워 두면 확인할 것이
         * 없는 동의 화면에 갇힌다.
         */
        private suspend fun initializeStages(stages: List<TermStage>) {
            if (stages.isEmpty()) return
            stageCandidates = stages
            // 열 때마다 처음부터 받는다. 이전에 열었을 때의 목록을 그대로 쓰면, 그 사이 기록된
            // 동의가 반영되지 않아 이미 끝난 항목을 다시 확인하게 된다.
            updateState {
                copy(
                    isStageMode = true,
                    stageDocuments = emptyList(),
                    checkedStageTerms = emptySet(),
                    isSubmitting = false,
                    errorMessage = null,
                )
            }

            val documents =
                stages
                    .mapNotNull { stage -> coordinator.requirementOf(stage).getOrNull() }
                    .flatMap { it.pending }
                    .distinctBy { it.termType }
            if (documents.isEmpty()) {
                navigationHelper.navigateToBack()
                return
            }
            updateState { copy(stageDocuments = documents, checkedStageTerms = emptySet()) }
        }

        private suspend fun toggleStageTerm(termType: TermType) {
            if (state.value.isSubmitting) return
            updateState {
                copy(
                    checkedStageTerms =
                        if (termType in checkedStageTerms) checkedStageTerms - termType else checkedStageTerms + termType,
                )
            }
        }

        /**
         * 이용약관 동의를 등록한다.
         *
         * 성공해도 화면을 옮기지 않는다 — 판정이 통과로 바뀌면 앱 루트가 스스로 다음으로 간다.
         * 여기서 이동까지 지시하면 같은 결정을 두 곳에서 하게 된다.
         */
        private suspend fun agree() {
            if (!state.value.canAgree) return
            if (state.value.isStageMode) {
                agreeStageTerms()
                return
            }
            val document = state.value.termsOfService ?: return
            updateState { copy(isSubmitting = true, errorMessage = null) }

            val error = coordinator.agree(listOf(document)).exceptionOrNull()
            // 개정 경쟁이면 새 버전으로 다시 보내지 않는다. 사용자가 읽지 않은 내용에 동의한
            // 기록이 서버에 남는다. 다시 조회해 바뀐 원문을 화면에 싣고 처음부터 다시 받는다.
            if (error is StaleTermVersionException) coordinator.refresh()

            updateState {
                copy(
                    isSubmitting = false,
                    errorMessage =
                        when (error) {
                            null -> null
                            is StaleTermVersionException -> REVISED_MESSAGE
                            else -> FAILURE_MESSAGE
                        },
                )
            }
        }

        /**
         * 단계 동의를 등록하고 돌아간다.
         *
         * 성공하면 화면을 닫는다 — 이 화면은 앱 루트가 아니라 밀어 넣은 화면이라 스스로 갈리지
         * 않는다. 생성은 **자동으로 재개하지 않는다**. 돌아간 자리의 전송 스냅샷이 그대로 남아
         * 있어 사진을 다시 고를 일은 없고, 동의를 누른 직후 업로드가 시작되면 취소할 틈이 없다.
         */
        private suspend fun agreeStageTerms() {
            val documents = state.value.stageDocuments
            updateState { copy(isSubmitting = true, errorMessage = null) }

            val error = coordinator.agree(documents).exceptionOrNull()
            if (error == null) {
                updateState { copy(isSubmitting = false) }
                navigationHelper.navigateToBack()
                return
            }
            // 개정 경쟁이면 새 버전으로 다시 보내지 않는다. 바뀐 문서를 다시 조회해 처음부터 받는다.
            val revised =
                if (error is StaleTermVersionException) {
                    coordinator.refresh()
                    stageCandidates
                        .mapNotNull { stage -> coordinator.requirementOf(stage).getOrNull() }
                        .flatMap { it.pending }
                        .distinctBy { it.termType }
                } else {
                    state.value.stageDocuments
                }
            updateState {
                copy(
                    isSubmitting = false,
                    stageDocuments = revised,
                    checkedStageTerms = emptySet(),
                    errorMessage = if (error is StaleTermVersionException) REVISED_MESSAGE else FAILURE_MESSAGE,
                )
            }
        }

        private suspend fun retry() {
            updateState { copy(errorMessage = null) }
            coordinator.refresh()
        }

        private suspend fun signOut() {
            updateState { copy(isSubmitting = true) }
            runCatching { logout() }
            updateState { copy(isSubmitting = false) }
        }

        private suspend fun loadPrivacyPolicy() {
            if (state.value.privacyPolicy != null) return
            val document = coordinator.documentOf(TermType.PRIVACY_POLICY) ?: return
            updateState { copy(privacyPolicy = document) }
        }

        private companion object {
            const val REVISED_MESSAGE = "약관이 개정돼 다시 확인이 필요해요. 내용을 확인하고 동의해 주세요."
            const val FAILURE_MESSAGE = "동의를 기록하지 못했어요. 잠시 후 다시 시도해 주세요."
        }
    }
