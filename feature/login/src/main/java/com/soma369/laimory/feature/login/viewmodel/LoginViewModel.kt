package com.soma369.laimory.feature.login.viewmodel

import androidx.lifecycle.viewModelScope
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.SocialLoginException
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.helper.SocialLoginCallbackHandler
import com.soma369.laimory.core.domain.model.auth.SocialLoginCallback
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.usecase.auth.CancelSocialLoginUseCase
import com.soma369.laimory.core.domain.usecase.auth.CompleteSocialLoginUseCase
import com.soma369.laimory.core.domain.usecase.auth.StartSocialLoginUseCase
import com.soma369.laimory.core.domain.usecase.terms.GetPublicTermLinksUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.login.state.LoginPhase
import com.soma369.laimory.feature.login.state.LoginUiIntent
import com.soma369.laimory.feature.login.state.LoginUiSideEffect
import com.soma369.laimory.feature.login.state.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val startSocialLogin: StartSocialLoginUseCase,
        private val completeSocialLogin: CompleteSocialLoginUseCase,
        private val cancelSocialLogin: CancelSocialLoginUseCase,
        private val callbackHandler: SocialLoginCallbackHandler,
        private val navigationHelper: NavigationHelper,
        private val getPublicTermLinks: GetPublicTermLinksUseCase,
    ) : BaseMviViewModel<LoginUiState, LoginUiIntent, LoginUiSideEffect>(LoginUiState()) {
        private var cancelDetectionJob: Job? = null

        init {
            viewModelScope.launch {
                callbackHandler.callbacks.collect { callback ->
                    sendIntent(LoginUiIntent.CallbackReceived(callback))
                }
            }
            // 약관 주소는 로그인과 무관한 곁가지다. 실패해도 링크만 비고 로그인은 그대로 된다.
            viewModelScope.launch {
                val links = getPublicTermLinks()
                updateState { copy(termLinks = links) }
            }
        }

        override suspend fun handleIntent(intent: LoginUiIntent) {
            try {
                when (intent) {
                    is LoginUiIntent.ProviderClicked -> startLogin(intent.provider)
                    is LoginUiIntent.CallbackReceived -> completeLogin(intent.callback)
                    LoginUiIntent.BrowserReturnedWithoutCallback -> scheduleCancellationCheck()
                    LoginUiIntent.AuthorizationLaunchFailed -> handleAuthorizationLaunchFailure()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showFailure(error)
            }
        }

        private suspend fun startLogin(provider: SocialLoginProvider) {
            if (state.value.isInteractionDisabled) return
            cancelDetectionJob?.cancel()
            updateState {
                copy(
                    phase = LoginPhase.PREPARING,
                    activeProvider = provider,
                    errorMessage = null,
                )
            }

            startSocialLogin(provider)
                .onSuccess { attempt ->
                    updateState { copy(phase = LoginPhase.WAITING_CALLBACK) }
                    sendEffect(LoginUiSideEffect.OpenAuthorizationUrl(attempt.authorizationUrl))
                }.onFailure(::showFailure)
        }

        private suspend fun completeLogin(callback: SocialLoginCallback) {
            if (state.value.phase == LoginPhase.EXCHANGING_TOKEN) return
            val previousState = state.value
            cancelDetectionJob?.cancel()
            updateState { copy(phase = LoginPhase.EXCHANGING_TOKEN, errorMessage = null) }

            completeSocialLogin(callback)
                .onSuccess {
                    updateState { copy(phase = LoginPhase.IDLE, activeProvider = null) }
                    navigationHelper.replaceRoot(HomePage)
                }.onFailure { error ->
                    if (error is SocialLoginException.MissingAttempt && previousState.phase == LoginPhase.IDLE) {
                        updateState { previousState }
                    } else {
                        showFailure(error)
                    }
                }
        }

        private suspend fun handleAuthorizationLaunchFailure() {
            cancelDetectionJob?.cancel()
            cancelSocialLogin()
            updateState {
                copy(
                    phase = LoginPhase.IDLE,
                    activeProvider = null,
                    errorMessage = BROWSER_UNAVAILABLE_MESSAGE,
                )
            }
        }

        private fun scheduleCancellationCheck() {
            cancelDetectionJob?.cancel()
            cancelDetectionJob =
                viewModelScope.launch {
                    // onNewIntent가 callback을 전달하는 짧은 구간과 Custom Tab 복귀 이벤트의 경합을 피한다.
                    delay(CALLBACK_GRACE_PERIOD_MILLIS)
                    if (state.value.phase != LoginPhase.WAITING_CALLBACK) return@launch
                    try {
                        cancelSocialLogin()
                        updateState { copy(phase = LoginPhase.IDLE, activeProvider = null) }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        showFailure(error)
                    }
                }
        }

        private fun showFailure(error: Throwable) {
            updateState {
                copy(
                    phase = LoginPhase.IDLE,
                    activeProvider = null,
                    errorMessage = error.toUserMessage(),
                )
            }
        }

        private fun Throwable.toUserMessage(): String =
            when (this) {
                is SocialLoginException -> message.orEmpty()
                is ApiException.NetworkException -> ApiException.NETWORK_ERROR
                is ApiException.UnauthorizedException ->
                    if (errorCode == EXPIRED_APP_CODE) {
                        "로그인 요청이 만료되었습니다. 다시 시도해 주세요."
                    } else {
                        message
                    }
                is ApiException -> message
                else -> ApiException.UNKNOWN_ERROR
            }

        private companion object {
            const val CALLBACK_GRACE_PERIOD_MILLIS = 500L
            const val EXPIRED_APP_CODE = -2002
            const val BROWSER_UNAVAILABLE_MESSAGE = "로그인을 진행할 브라우저를 찾을 수 없습니다."
        }
    }
