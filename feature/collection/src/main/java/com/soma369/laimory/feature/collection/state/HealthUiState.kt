package com.soma369.laimory.feature.collection.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class HealthUiState(
    /** 저장소 첫 emission 전까지 로딩으로 둔다. (빈 상태 문구 선노출 방지) */
    val isLoading: Boolean = true,
    /** 스테이징된 건강 아이템(저장소 관찰 결과에서 HEALTH 만 필터). */
    val stagedHealth: List<SourceItem> = emptyList(),
    /** 수집이 진행 중인지. 버튼 비활성/진행표시에 사용. */
    val isBusy: Boolean = false,
) : UiState
