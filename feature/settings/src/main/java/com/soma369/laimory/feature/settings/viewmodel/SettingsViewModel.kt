package com.soma369.laimory.feature.settings.viewmodel

import androidx.lifecycle.viewModelScope
import com.soma369.laimory.core.domain.helper.GlobalLoadingHelper
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.DialogRequest
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.domain.navigation.LoginPage
import com.soma369.laimory.core.domain.usecase.auth.LogoutUseCase
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import com.soma369.laimory.core.domain.usecase.user.ObserveUserProfileUseCase
import com.soma369.laimory.core.domain.usecase.user.RefreshUserProfileUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.settings.state.SettingsUiIntent
import com.soma369.laimory.feature.settings.state.SettingsUiSideEffect
import com.soma369.laimory.feature.settings.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val logoutUseCase: LogoutUseCase,
        observeSignedInAccount: ObserveSignedInAccountUseCase,
        private val observeUserProfileUseCase: ObserveUserProfileUseCase,
        private val refreshUserProfileUseCase: RefreshUserProfileUseCase,
        private val navigationHelper: NavigationHelper,
        private val messageHelper: MessageHelper,
        private val globalLoadingHelper: GlobalLoadingHelper,
    ) : BaseMviViewModel<SettingsUiState, SettingsUiIntent, SettingsUiSideEffect>(SettingsUiState()) {
        private var logoutConfirmJob: Job? = null

        init {
            viewModelScope.launch {
                observeSignedInAccount().collect { account ->
                    updateState {
                        if (account == null) {
                            copy(accountProvider = null)
                        } else {
                            // isLoggingOut은 재인증된 계정을 관찰할 때 해제해 대기 중인 중복 확인도 차단한다.
                            copy(accountProvider = account.provider, isLoggingOut = false)
                        }
                    }
                }
            }
            observeUserProfile()
        }

        /**
         * 공용 회원 정보를 계정 카드에 반영한다.
         *
         * 닉네임 비우기는 coordinator 가 세션 전이에서 하므로 여기서 따로 지우지 않는다 — 로그아웃하면
         * 공용 상태가 null 이 되어 이 흐름으로 함께 내려온다.
         */
        private fun observeUserProfile() {
            refreshUserProfileUseCase()
            viewModelScope.launch {
                observeUserProfileUseCase().collect { profile ->
                    updateState { copy(nickname = profile?.nickname) }
                }
            }
        }

        override suspend fun handleIntent(intent: SettingsUiIntent) {
            when (intent) {
                SettingsUiIntent.LogoutClicked -> requestLogoutConfirm()
                SettingsUiIntent.LogoutDismissed -> Unit
                SettingsUiIntent.LogoutConfirmed -> logout()
            }
        }

        /**
         * 공통 Dialog로 로그아웃 확인을 요청하고 결과를 화면 Intent로 변환한다.
         *
         * intent consumer를 막지 않도록 별도 Job에서 결과를 기다리며, Job 가드와
         * 헬퍼의 활성 단일 정책이 연속 탭의 중복 Dialog를 함께 막는다.
         */
        private fun requestLogoutConfirm() {
            if (state.value.isLoggingOut) return
            if (logoutConfirmJob?.isActive == true) return
            logoutConfirmJob =
                safeLaunch(onError = { error -> if (error !is CancellationException) handleFailure(error) }) {
                    val result =
                        messageHelper.showTwoButtonDialog(
                            DialogRequest.TwoButton(
                                title = "로그아웃할까요?",
                                body = "이 기기에서 로그아웃하고 로그인 화면으로 이동합니다.",
                                primaryLabel = "로그아웃",
                                secondaryLabel = "취소",
                            ),
                        )
                    sendIntent(
                        when (result) {
                            DialogResult.Primary -> SettingsUiIntent.LogoutConfirmed
                            DialogResult.Secondary, DialogResult.Dismissed -> SettingsUiIntent.LogoutDismissed
                        },
                    )
                }
        }

        private suspend fun logout() {
            if (state.value.isLoggingOut) return
            updateState { copy(isLoggingOut = true) }
            try {
                // 로그아웃과 인증 Root 교체 동안 앱 전체 입력을 전역 로딩으로 차단한다.
                globalLoadingHelper.withLoading(LOGOUT_LOADING_KEY) {
                    logoutUseCase()
                    navigationHelper.replaceRoot(LoginPage)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                updateState { copy(isLoggingOut = false) }
                handleFailure(error)
            }
        }

        private companion object {
            const val LOGOUT_LOADING_KEY = "settings-logout"
        }
    }
