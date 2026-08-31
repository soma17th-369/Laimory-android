package com.soma369.laimory.feature.terms.viewmodel

import androidx.lifecycle.viewModelScope
import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.exception.StaleTermVersionException
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
    ) : BaseMviViewModel<TermsUiState, TermsUiIntent, TermsUiSideEffect>(TermsUiState()) {
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
                is TermsUiIntent.AgeConfirmationChanged -> updateState { copy(isAgeConfirmed = intent.isConfirmed) }
                TermsUiIntent.AgreeClicked -> agree()
                TermsUiIntent.RetryClicked -> retry()
                TermsUiIntent.LogoutClicked -> signOut()
            }
        }

        /**
         * 이용약관 동의를 등록한다.
         *
         * 성공해도 화면을 옮기지 않는다 — 판정이 통과로 바뀌면 앱 루트가 스스로 다음으로 간다.
         * 여기서 이동까지 지시하면 같은 결정을 두 곳에서 하게 된다.
         */
        private suspend fun agree() {
            val document = state.value.termsOfService ?: return
            if (!state.value.canAgree) return
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
