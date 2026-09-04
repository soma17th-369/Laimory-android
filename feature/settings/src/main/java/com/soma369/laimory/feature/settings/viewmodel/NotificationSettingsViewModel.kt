package com.soma369.laimory.feature.settings.viewmodel

import androidx.lifecycle.viewModelScope
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.push.PushSettings
import com.soma369.laimory.core.domain.usecase.push.GetPushSettingsUseCase
import com.soma369.laimory.core.domain.usecase.push.UpdateDailyReminderEnabledUseCase
import com.soma369.laimory.core.domain.usecase.push.UpdatePushEnabledUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiContent
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiIntent
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiSideEffect
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiState
import com.soma369.laimory.feature.settings.state.NotificationToggle
import com.soma369.laimory.feature.settings.state.isEnabled
import com.soma369.laimory.feature.settings.state.with
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 알림 수신 설정.
 *
 * 값의 주인은 서버다 — 기기에 복제하지 않고, 켤 때마다 서버에 그대로 보낸다. 다만 화면은 누른
 * 즉시 반응한다. 응답을 기다렸다 바꾸면 누른 자리가 움직이지 않아 눌리지 않은 것처럼 보인다.
 * 서버가 거절하면 서버 값으로 되돌리고 알린다 — 켜 둔 적 없는 설정을 켜져 있다고 믿게 두지 않는다.
 *
 * 연타는 마지막 값 하나로 모아 보낸다([COMMIT_DEBOUNCE_MILLIS]). 중간 값까지 보내면 켰다 껐다가
 * 그대로 서버에 가고, 응답이 보낸 순서대로 온다는 보장이 없어 화면과 서버가 어긋날 수 있다.
 */
@HiltViewModel
class NotificationSettingsViewModel
    @Inject
    constructor(
        private val getPushSettingsUseCase: GetPushSettingsUseCase,
        private val updatePushEnabledUseCase: UpdatePushEnabledUseCase,
        private val updateDailyReminderEnabledUseCase: UpdateDailyReminderEnabledUseCase,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<NotificationSettingsUiState, NotificationSettingsUiIntent, NotificationSettingsUiSideEffect>(
            NotificationSettingsUiState(),
        ) {
        /** 줄마다 하나. 같은 줄을 다시 누르면 취소하고 처음부터 다시 센다. */
        private val commitJobs = mutableMapOf<NotificationToggle, Job>()

        override suspend fun handleIntent(intent: NotificationSettingsUiIntent) {
            when (intent) {
                NotificationSettingsUiIntent.Initialize -> loadIfNeeded()
                NotificationSettingsUiIntent.RetryLoad -> load()
                NotificationSettingsUiIntent.NavigateBack -> navigationHelper.navigateToBack()
                is NotificationSettingsUiIntent.ToggleChanged -> onToggleChanged(intent.toggle, intent.isEnabled)
            }
        }

        /** 화면 복귀마다 다시 묻지 않는다. 값을 이미 들고 있으면 그대로 쓴다. */
        private suspend fun loadIfNeeded() {
            if (state.value.content is NotificationSettingsUiContent.Settings) return
            load()
        }

        private suspend fun load() {
            updateState { copy(content = NotificationSettingsUiContent.Loading) }
            getPushSettingsUseCase()
                .onSuccess { settings ->
                    updateState {
                        copy(
                            content = NotificationSettingsUiContent.Settings(settings),
                            confirmedSettings = settings,
                        )
                    }
                }.onFailure {
                    updateState {
                        copy(content = NotificationSettingsUiContent.LoadFailed, confirmedSettings = null)
                    }
                }
        }

        private fun onToggleChanged(
            toggle: NotificationToggle,
            isEnabled: Boolean,
        ) {
            val shown = state.value.settings ?: return
            // 전체가 꺼져 있으면 하위 줄은 손댈 수 없다. 화면에서도 잠그지만 Intent 경로도 막는다.
            if (toggle == NotificationToggle.DAILY_REMINDER && !shown.isPushEnabled) return
            if (isEnabled == shown.isEnabled(toggle)) return

            showSettings(shown.with(toggle, isEnabled))
            // 전체를 끄면 하위 줄은 눌러도 오지 않는 알림이 된다. 아직 보내지 않은 변경은 사용자가
            // 더 볼 수 없는 값이므로 여기서 버린다.
            if (toggle == NotificationToggle.PUSH && !isEnabled) {
                dropPending(NotificationToggle.DAILY_REMINDER)
            }

            commitJobs[toggle]?.cancel()
            // safeLaunch 는 취소까지 runCatching 으로 잡아 실패로 알린다. 취소는 다음 누름이
            // 이어받았다는 뜻이라 알릴 것이 없으므로 직접 띄운다.
            commitJobs[toggle] =
                viewModelScope.launch {
                    delay(COMMIT_DEBOUNCE_MILLIS)
                    commit(toggle)
                }
        }

        /** 손이 멈춘 뒤 마지막 값 하나만 보낸다. */
        private suspend fun commit(toggle: NotificationToggle) {
            val current = state.value
            val shown = current.settings ?: return
            val confirmed = current.confirmedSettings ?: return
            val isEnabled = shown.isEnabled(toggle)
            // 눌렀다 제자리로 돌아왔으면 보낼 것이 없다.
            if (isEnabled == confirmed.isEnabled(toggle)) return

            val result =
                when (toggle) {
                    NotificationToggle.PUSH -> updatePushEnabledUseCase(isEnabled)
                    NotificationToggle.DAILY_REMINDER -> updateDailyReminderEnabledUseCase(isEnabled)
                }
            result
                .onSuccess {
                    updateState { copy(confirmedSettings = confirmedSettings?.with(toggle, isEnabled)) }
                }.onFailure { error ->
                    dropPending(toggle)
                    handleUpdateFailure(error)
                }
        }

        /** 보내지 않기로 한 변경을 거두고 화면을 서버 값으로 되돌린다. */
        private fun dropPending(toggle: NotificationToggle) {
            commitJobs.remove(toggle)?.cancel()
            updateState {
                val shown = settings ?: return@updateState this
                val confirmed = confirmedSettings ?: return@updateState this
                copy(
                    content =
                        NotificationSettingsUiContent.Settings(
                            shown.with(toggle, confirmed.isEnabled(toggle)),
                        ),
                )
            }
        }

        /** 화면값만 바꾼다. 서버가 확인한 값은 그대로 둔다. */
        private fun showSettings(settings: PushSettings) {
            updateState { copy(content = NotificationSettingsUiContent.Settings(settings)) }
        }

        private fun handleUpdateFailure(error: Throwable) {
            if (error is HandledException) return
            val message =
                if (error is ApiException.NetworkException) {
                    "네트워크 상태를 확인한 뒤 다시 시도해주세요."
                } else {
                    "설정을 바꾸지 못했어요. 잠시 후 다시 시도해주세요."
                }
            sendEffect(NotificationSettingsUiSideEffect.ShowSnackbar(message))
        }

        private companion object {
            /**
             * 손이 멈췄다고 볼 시간.
             *
             * 연타를 한 번으로 모을 만큼은 길고, 한 번만 누르고 화면을 떠나는 사람이 기다림을
             * 느끼지 않을 만큼은 짧게 둔다. 화면을 떠나도 보내던 것은 끝난다 — 이 ViewModel 은
             * 화면이 아니라 Activity 에 매여 있다.
             */
            const val COMMIT_DEBOUNCE_MILLIS = 400L
        }
    }
