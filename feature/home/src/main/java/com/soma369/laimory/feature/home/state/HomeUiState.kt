package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class HomeUiState(
    val counter: Int = 0,
    val isLoading: Boolean = false,
) : UiState
