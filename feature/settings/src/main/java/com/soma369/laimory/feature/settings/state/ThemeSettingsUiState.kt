package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.settings.AppThemeMode
import com.soma369.laimory.core.ui.base.UiState

/**
 * @param selected 저장된 화면 모드. `null` 은 아직 읽기 전이다 — 그동안은 어느 줄에도 체크를
 *   두지 않는다. 기본값을 미리 찍으면 저장값이 도착하는 순간 체크가 옮겨 다닌다.
 */
@Immutable
data class ThemeSettingsUiState(
    val selected: AppThemeMode? = null,
) : UiState
