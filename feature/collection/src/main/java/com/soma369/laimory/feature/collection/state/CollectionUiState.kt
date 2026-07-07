package com.soma369.laimory.feature.collection.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class CollectionUiState(
    /** 저장소 첫 emission 전까지 로딩으로 둔다. (빈 상태 문구 선노출 방지) */
    val isLoading: Boolean = true,
    val items: List<SourceItem> = emptyList(),
) : UiState
