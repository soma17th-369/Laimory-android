package com.soma369.laimory.feature.settings.viewmodel

import androidx.lifecycle.viewModelScope
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.navigation.LoginPage
import com.soma369.laimory.core.domain.usecase.auth.LogoutUseCase
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.settings.state.SettingsUiIntent
import com.soma369.laimory.feature.settings.state.SettingsUiSideEffect
import com.soma369.laimory.feature.settings.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val logoutUseCase: LogoutUseCase,
        observeSignedInAccount: ObserveSignedInAccountUseCase,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<SettingsUiState, SettingsUiIntent, SettingsUiSideEffect>(SettingsUiState()) {
        init {
            viewModelScope.launch {
                observeSignedInAccount().collect { account ->
                    updateState {
                        if (account == null) {
                            copy(accountProvider = null)
                        } else {
                            copy(
                                accountProvider = account.provider,
                                isLogoutDialogVisible = false,
                                isLoggingOut = false,
                            )
                        }
                    }
                }
            }
        }

        override suspend fun handleIntent(intent: SettingsUiIntent) {
            when (intent) {
                SettingsUiIntent.LogoutClicked -> showLogoutDialog()
                SettingsUiIntent.LogoutDismissed -> dismissLogoutDialog()
                SettingsUiIntent.LogoutConfirmed -> logout()
            }
        }

        private fun showLogoutDialog() {
            if (state.value.isLoggingOut) return
            updateState { copy(isLogoutDialogVisible = true) }
        }

        private fun dismissLogoutDialog() {
            if (state.value.isLoggingOut) return
            updateState { copy(isLogoutDialogVisible = false) }
        }

        private suspend fun logout() {
            if (state.value.isLoggingOut) return
            updateState { copy(isLoggingOut = true) }
            try {
                logoutUseCase()
                // NavigationEntry가 재사용되어도 이전 로그아웃 다이얼로그를 다시 노출하지 않는다.
                // isLoggingOut은 재인증된 계정을 관찰할 때 해제해 대기 중인 중복 확인도 차단한다.
                updateState { copy(isLogoutDialogVisible = false) }
                navigationHelper.replaceRoot(LoginPage)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                updateState { copy(isLogoutDialogVisible = false, isLoggingOut = false) }
                handleFailure(error)
            }
        }
    }
