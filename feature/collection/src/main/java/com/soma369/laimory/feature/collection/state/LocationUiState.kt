package com.soma369.laimory.feature.collection.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class LocationUiState(
    /** 저장소 첫 emission 전까지 로딩으로 둔다. */
    val isLoading: Boolean = true,
    /** 위치 자동 수집(추적) 활성 여부. */
    val isTracking: Boolean = false,
    /** 스테이징된 위치 계열 아이템(LOCATION 체류 + MOVEMENT 이동). */
    val stagedItems: List<SourceItem> = emptyList(),
) : UiState
