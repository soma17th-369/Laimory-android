package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.IntroInfo
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class HomeUiState(
    val isLoading: Boolean = false,
    val introInfo: IntroInfo? = null,
) : UiState
