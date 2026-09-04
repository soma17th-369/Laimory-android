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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
 *
 * **모으는 것은 아직 보내지 않은 누름까지다.** 한 번 나간 요청은 취소하지 않고([commitLock]),
 * 그 뒤에 온 누름은 줄을 서서 다시 보낸다 — 끊으면 서버에 닿았는지 모르는 채 남고, 그사이 화면이
 * 원래 값으로 돌아와 있으면 보낼 것이 없다고 판단해 서버만 반대로 켜진 채 갈라진다.
 *
 * 고른 값을 **대신 거두는 자리는 두지 않는다.** 화면값을 서버로 보내는 경로 밖에서 되돌리면, 이미
 * 나간 요청이 성공했을 때 서버는 켜고 화면은 꺼진 채로 남는다. 화면값을 바꾸는 것은 사용자의
 * 누름과 서버의 거절뿐이다.
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
        private val debounceJobs = mutableMapOf<NotificationToggle, Job>()

        /**
         * 서버로 나가는 일을 한 줄로 세운다.
         *
         * 보내는 중에 다음 요청이 끼어들면 응답 순서가 보낸 순서와 달라져 나중 값이 먼저 확정될 수
         * 있다. 조회도 같은 줄에 세운다 — 겹치면 방금 보낸 값이 빠진 응답으로 화면을 덮는다.
         */
        private val commitLock = Mutex()

        override suspend fun handleIntent(intent: NotificationSettingsUiIntent) {
            when (intent) {
                NotificationSettingsUiIntent.Initialize -> load()
                NotificationSettingsUiIntent.RetryLoad -> load()
                NotificationSettingsUiIntent.NavigateBack -> navigationHelper.navigateToBack()
                is NotificationSettingsUiIntent.ToggleChanged -> onToggleChanged(intent.toggle, intent.isEnabled)
            }
        }

        /**
         * 화면에 들어올 때마다 서버에 다시 묻는다.
         *
         * 들고 있던 값을 그대로 쓰지 않는다. 이 ViewModel 은 화면이 아니라 **Activity 에 매여
         * 있어**(NavDisplay 의 기본 구성에는 entry 별 ViewModelStore 가 없다) 로그아웃하고 다른
         * 계정으로 들어와도 살아 있다. 그때 이전 계정의 설정을 보여 주면, 사용자는 그것이 지금
         * 계정의 값인 줄 알고 그 위에서 바꾼다. 다른 기기에서 바꾼 값도 같은 이유로 여기서 따라온다.
         *
         * 아직 보내지 않은 변경은 이 조회가 정리한다 — 화면값이 서버 값으로 갈리므로, 뒤늦게 깨어난
         * 디바운스는 보낼 것을 찾지 못한다.
         */
        private suspend fun load() {
            updateState { copy(content = NotificationSettingsUiContent.Loading) }
            commitLock.withLock { getPushSettingsUseCase() }
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

            // 취소하는 것은 **기다리는 중**인 누름뿐이다. 줄마다 따로 세므로 전체 알림을 끄는 것이
            // 하위 줄의 대기를 건드리지 않는다 — 방금 고른 값은 잠긴 줄에 그대로 남고 서버에도
            // 그대로 간다. 다시 켰을 때 종전 선택이 살아 있어야 하는 것이 이 화면의 규칙이다.
            debounceJobs[toggle]?.cancel()
            // safeLaunch 는 취소까지 runCatching 으로 잡아 실패로 알린다. 취소는 다음 누름이
            // 이어받았다는 뜻이라 알릴 것이 없으므로 직접 띄운다.
            debounceJobs[toggle] =
                viewModelScope.launch {
                    delay(COMMIT_DEBOUNCE_MILLIS)
                    // 대기를 넘긴 뒤에는 취소되지 않는다. 다음 누름은 이 요청이 끝난 뒤 줄을 서서
                    // 마지막 값을 다시 보낸다.
                    withContext(NonCancellable) { commitLock.withLock { commit(toggle) } }
                }
        }

        /** 화면에 떠 있는 값과 서버가 확인해 준 값이 다르면 그 차이를 보낸다. */
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
                    // 보내는 사이 사용자가 다시 눌렀으면 그 값이 최신이다. 되돌리면 방금 한 조작을
                    // 뒤엎게 되고, 뒤이어 줄 서 있는 요청이 어차피 그 값을 보낸다.
                    if (state.value.settings?.isEnabled(toggle) == isEnabled) revertToConfirmed(toggle)
                    handleUpdateFailure(error)
                }
        }

        /** 화면을 서버가 확인해 준 값으로 되돌린다. */
        private fun revertToConfirmed(toggle: NotificationToggle) {
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
