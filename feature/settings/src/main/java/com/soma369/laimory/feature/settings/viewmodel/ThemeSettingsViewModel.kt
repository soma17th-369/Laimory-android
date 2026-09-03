package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.settings.AppThemeMode
import com.soma369.laimory.core.domain.usecase.settings.ObserveAppThemeModeUseCase
import com.soma369.laimory.core.domain.usecase.settings.SetAppThemeModeUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.settings.state.ThemeSettingsUiIntent
import com.soma369.laimory.feature.settings.state.ThemeSettingsUiSideEffect
import com.soma369.laimory.feature.settings.state.ThemeSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 화면 모드 선택.
 *
 * 표시값은 저장소 흐름 하나만 본다 — 고른 값을 화면이 따로 기억하지 않으므로, 저장에 실패하면
 * 체크는 저절로 이전 자리에 남는다. 앱 루트도 같은 흐름을 보고 있어 저장되는 순간 전체가 바뀐다.
 */
@HiltViewModel
class ThemeSettingsViewModel
    @Inject
    constructor(
        observeAppThemeModeUseCase: ObserveAppThemeModeUseCase,
        private val setAppThemeModeUseCase: SetAppThemeModeUseCase,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<ThemeSettingsUiState, ThemeSettingsUiIntent, ThemeSettingsUiSideEffect>(
            ThemeSettingsUiState(),
        ) {
        init {
            safeLaunch {
                observeAppThemeModeUseCase().collect { mode ->
                    updateState { copy(selected = mode) }
                }
            }
        }

        override suspend fun handleIntent(intent: ThemeSettingsUiIntent) {
            when (intent) {
                ThemeSettingsUiIntent.NavigateBack -> navigationHelper.navigateToBack()
                is ThemeSettingsUiIntent.Select -> select(intent.mode)
            }
        }

        private suspend fun select(mode: AppThemeMode) {
            if (state.value.selected == mode) return
            setAppThemeModeUseCase(mode).onFailure {
                sendEffect(ThemeSettingsUiSideEffect.ShowSnackbar("설정을 저장하지 못했어요. 잠시 후 다시 시도해주세요."))
            }
        }
    }
