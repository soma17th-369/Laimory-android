package com.soma369.laimory.feature.settings.viewmodel

import androidx.lifecycle.viewModelScope
import com.soma369.laimory.core.domain.helper.GlobalLoadingHelper
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.DialogActionStyle
import com.soma369.laimory.core.domain.message.DialogRequest
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.user.AccountWithdrawalOutcome
import com.soma369.laimory.core.domain.navigation.LoginPage
import com.soma369.laimory.core.domain.navigation.NotificationSettingsPage
import com.soma369.laimory.core.domain.usecase.auth.LogoutUseCase
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import com.soma369.laimory.core.domain.usecase.terms.GetPublicTermLinksUseCase
import com.soma369.laimory.core.domain.usecase.user.ObserveUserProfileUseCase
import com.soma369.laimory.core.domain.usecase.user.RefreshUserProfileUseCase
import com.soma369.laimory.core.domain.usecase.user.WithdrawAccountUseCase
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
        private val withdrawAccountUseCase: WithdrawAccountUseCase,
        observeSignedInAccount: ObserveSignedInAccountUseCase,
        private val observeUserProfileUseCase: ObserveUserProfileUseCase,
        private val refreshUserProfileUseCase: RefreshUserProfileUseCase,
        private val navigationHelper: NavigationHelper,
        private val messageHelper: MessageHelper,
        private val globalLoadingHelper: GlobalLoadingHelper,
        private val getPublicTermLinks: GetPublicTermLinksUseCase,
    ) : BaseMviViewModel<SettingsUiState, SettingsUiIntent, SettingsUiSideEffect>(SettingsUiState()) {
        private var logoutConfirmJob: Job? = null
        private var accountDeleteConfirmJob: Job? = null

        init {
            viewModelScope.launch {
                observeSignedInAccount().collect { account ->
                    updateState {
                        if (account == null) {
                            copy(accountProvider = null)
                        } else {
                            // 진행 상태는 재인증된 계정을 관찰할 때 해제한다. ViewModel 이 Activity 수명이라
                            // 여기서 안 지우면 새 계정으로 로그인한 뒤에도 계정 항목이 잠긴 채 남는다.
                            copy(accountProvider = account.provider, isLoggingOut = false, isWithdrawing = false)
                        }
                    }
                }
            }
            observeUserProfile()
        }

        /**
         * 약관 주소를 다시 받아 온다. 이미 받았으면 아무것도 하지 않는다.
         *
         * 닉네임과 같은 이유로 화면이 뜰 때마다 부른다 — ViewModel 이 Activity 수명이라 init 에서
         * 한 번만 부르면 첫 조회가 실패한 세션 내내 눌리지 않는 줄로 남는다. 게시된 문서는 앱이
         * 도는 동안 바뀌지 않으므로 한 번 받으면 다시 묻지 않는다.
         */
        private suspend fun refreshTermLinks() {
            if (state.value.hasTermLinks) return
            val links = getPublicTermLinks()
            updateState { copy(termLinks = links) }
        }

        /**
         * 공용 회원 정보를 계정 카드에 반영한다.
         *
         * 닉네임 비우기는 coordinator 가 세션 전이에서 하므로 여기서 따로 지우지 않는다 — 로그아웃하면
         * 공용 상태가 null 이 되어 이 흐름으로 함께 내려온다. 재시도는
         * [SettingsUiIntent.RefreshProfile] 이 맡는다.
         */
        private fun observeUserProfile() {
            viewModelScope.launch {
                observeUserProfileUseCase().collect { profile ->
                    updateState { copy(nickname = profile?.nickname) }
                }
            }
        }

        override suspend fun handleIntent(intent: SettingsUiIntent) {
            when (intent) {
                // 화면이 뜰 때마다 부른다. ViewModel 이 Activity 수명이라 init 에서 한 번만 부르면
                // 첫 조회가 실패한 세션 내내 제공자 문구로 남는다.
                SettingsUiIntent.RefreshProfile -> refreshUserProfileUseCase()
                SettingsUiIntent.RefreshTermLinks -> refreshTermLinks()
                SettingsUiIntent.NotificationSettingsClicked ->
                    navigationHelper.navigateTo(NotificationSettingsPage)
                SettingsUiIntent.LogoutClicked -> requestLogoutConfirm()
                SettingsUiIntent.LogoutDismissed -> Unit
                SettingsUiIntent.LogoutConfirmed -> logout()
                SettingsUiIntent.AccountDeleteClicked -> requestAccountDeleteConfirm()
                SettingsUiIntent.AccountDeleteDismissed -> Unit
                SettingsUiIntent.AccountDeleteConfirmed -> withdrawAccount()
            }
        }

        /**
         * 공통 Dialog로 로그아웃 확인을 요청하고 결과를 화면 Intent로 변환한다.
         *
         * intent consumer를 막지 않도록 별도 Job에서 결과를 기다리며, Job 가드와
         * 헬퍼의 활성 단일 정책이 연속 탭의 중복 Dialog를 함께 막는다.
         */
        private fun requestLogoutConfirm() {
            if (state.value.isAccountActionInProgress) return
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

        /**
         * 확인 체크박스가 달린 공통 Dialog 로 계정 삭제 동의를 받는다.
         *
         * 체크 전에는 삭제 버튼이 잠기므로 [DialogResult.Primary] 가 곧 동의를 뜻한다 —
         * 여기서 체크 여부를 따로 확인하지 않는다.
         */
        private fun requestAccountDeleteConfirm() {
            if (state.value.isAccountActionInProgress) return
            if (accountDeleteConfirmJob?.isActive == true) return
            accountDeleteConfirmJob =
                safeLaunch(onError = { error -> if (error !is CancellationException) handleFailure(error) }) {
                    val result =
                        messageHelper.showConsentDialog(
                            DialogRequest.Consent(
                                title = ACCOUNT_DELETE_TITLE,
                                body = ACCOUNT_DELETE_BODY,
                                consentLabel = ACCOUNT_DELETE_CONSENT_LABEL,
                                primaryLabel = "계정 삭제",
                                secondaryLabel = "취소",
                                primaryStyle = DialogActionStyle.DESTRUCTIVE,
                            ),
                        )
                    sendIntent(
                        when (result) {
                            DialogResult.Primary -> SettingsUiIntent.AccountDeleteConfirmed
                            DialogResult.Secondary, DialogResult.Dismissed -> SettingsUiIntent.AccountDeleteDismissed
                        },
                    )
                }
        }

        /**
         * 탈퇴를 요청하고 로그인 Root 로 교체한다.
         *
         * 안내는 Root 교체 **뒤에** 공통 채널로 보낸다 — 이 화면은 그 시점에 이미 사라져 있어
         * 화면 스낵바로는 보이지 않는다.
         *
         * `401` 로 끝난 요청은 완료로 안내하지 않는다. 서버가 만료 세션과 이미 탈퇴한 회원을
         * 구분하지 않으므로 삭제됐다고 단정할 수 없다.
         */
        private suspend fun withdrawAccount() {
            if (state.value.isWithdrawing) return
            updateState { copy(isWithdrawing = true) }
            try {
                globalLoadingHelper.withLoading(WITHDRAW_LOADING_KEY) {
                    val outcome = withdrawAccountUseCase().getOrThrow()
                    navigationHelper.replaceRoot(LoginPage)
                    messageHelper.send(
                        when (outcome) {
                            AccountWithdrawalOutcome.Accepted -> UserMessage.AccountWithdrawalAccepted
                            AccountWithdrawalOutcome.SessionUnavailable -> UserMessage.AccountWithdrawalUnverified
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                updateState { copy(isWithdrawing = false) }
                handleFailure(error)
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
            const val WITHDRAW_LOADING_KEY = "settings-withdraw"

            const val ACCOUNT_DELETE_TITLE = "계정을 삭제할까요?"

            /**
             * 서버가 실제로 하는 일만 적는다.
             *
             * 탈퇴 접수는 논리 탈퇴와 삭제 작업 등록까지이고 물리 삭제는 이후에 처리되므로 "시간이
             * 걸릴 수 있다"를 밝힌다. 같은 소셜 계정으로 다시 로그인하면 서버가 이전 계정을 복구하지
             * 않고 새 회원을 만들므로 복구 불가를 명시한다. 기기에 남는 수집 원본은 탈퇴로 지워지지
             * 않으므로 지우는 방법을 함께 알린다.
             */
            const val ACCOUNT_DELETE_BODY =
                "계정과 서버에 저장된 기록이 모두 삭제되며 되돌릴 수 없습니다. " +
                    "삭제 처리에는 시간이 걸릴 수 있습니다.\n\n" +
                    "바로 로그아웃되고, 같은 계정으로 다시 로그인해도 이전 기록은 복구되지 않습니다. " +
                    "이 기기에 남아 있는 수집 기록은 앱을 삭제하면 지워집니다."

            const val ACCOUNT_DELETE_CONSENT_LABEL = "위 내용을 확인했으며 계정 삭제에 동의합니다"
        }
    }
