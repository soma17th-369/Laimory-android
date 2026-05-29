package com.soma369.laimory.feature.feature1.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.Feature1Item
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class Feature1UiState(
    val isLoading: Boolean = false,
    val items: List<Feature1Item> = emptyList(),
) : UiState
