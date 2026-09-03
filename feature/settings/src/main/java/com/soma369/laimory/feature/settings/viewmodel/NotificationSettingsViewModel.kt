package com.soma369.laimory.feature.settings.viewmodel

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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 알림 수신 설정.
 *
 * 서버가 단일 권위다 — 값을 기기에 복제하지 않고, 화면에 그리는 값은 조회 응답과 변경 성공만으로
 * 바뀐다. 미리 켠 것처럼 보여 준 뒤 실패하면 재설치·다른 기기에서 다르게 보이는 설정을 사용자가
 * 켜 뒀다고 믿게 된다.
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
        override suspend fun handleIntent(intent: NotificationSettingsUiIntent) {
            when (intent) {
                NotificationSettingsUiIntent.Initialize -> loadIfNeeded()
                NotificationSettingsUiIntent.RetryLoad -> load()
                NotificationSettingsUiIntent.NavigateBack -> navigationHelper.navigateToBack()
                is NotificationSettingsUiIntent.ToggleChanged -> updateToggle(intent.toggle, intent.isEnabled)
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
                    updateState { copy(content = NotificationSettingsUiContent.Settings(settings)) }
                }.onFailure {
                    updateState { copy(content = NotificationSettingsUiContent.LoadFailed) }
                }
        }

        private suspend fun updateToggle(
            toggle: NotificationToggle,
            isEnabled: Boolean,
        ) {
            val current = state.value
            val settings = current.settings ?: return
            if (current.isUpdating(toggle)) return
            // 전체가 꺼져 있으면 하위 줄은 손댈 수 없다. 화면에서도 잠그지만 Intent 경로도 막는다.
            if (toggle == NotificationToggle.DAILY_REMINDER && !settings.isPushEnabled) return
            if (isEnabled == settings.isEnabled(toggle)) return

            updateState { copy(updatingToggles = updatingToggles + toggle) }
            val result =
                when (toggle) {
                    NotificationToggle.PUSH -> updatePushEnabledUseCase(isEnabled)
                    NotificationToggle.DAILY_REMINDER -> updateDailyReminderEnabledUseCase(isEnabled)
                }
            result
                .onSuccess { applyToggle(toggle, isEnabled) }
                .onFailure(::handleUpdateFailure)
            updateState { copy(updatingToggles = updatingToggles - toggle) }
        }

        /** 서버가 받아들인 뒤에만 표시값을 바꾼다. */
        private suspend fun applyToggle(
            toggle: NotificationToggle,
            isEnabled: Boolean,
        ) {
            updateState {
                val settings = settings ?: return@updateState this
                val next =
                    when (toggle) {
                        NotificationToggle.PUSH -> settings.copy(isPushEnabled = isEnabled)
                        NotificationToggle.DAILY_REMINDER -> settings.copy(isDailyReminderEnabled = isEnabled)
                    }
                copy(content = NotificationSettingsUiContent.Settings(next))
            }
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

        private fun PushSettings.isEnabled(toggle: NotificationToggle): Boolean =
            when (toggle) {
                NotificationToggle.PUSH -> isPushEnabled
                NotificationToggle.DAILY_REMINDER -> isDailyReminderEnabled
            }
    }
