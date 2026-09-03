package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.domain.model.settings.AppThemeMode
import com.soma369.laimory.core.ui.base.UiIntent

sealed interface ThemeSettingsUiIntent : UiIntent {
    data object NavigateBack : ThemeSettingsUiIntent

    data class Select(
        val mode: AppThemeMode,
    ) : ThemeSettingsUiIntent
}
