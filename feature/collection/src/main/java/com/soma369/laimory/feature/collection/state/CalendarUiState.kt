package com.soma369.laimory.feature.collection.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class CalendarUiState(
    /** 한 번이라도 읽었는지. 읽기 전에는 "일정이 없다" 대신 조회 안내를 보인다. */
    val hasLoaded: Boolean = false,
    /** 마지막으로 읽은 일정. 저장하지 않으므로 탭을 다시 열면 비어 있다. */
    val events: List<SourceItem> = emptyList(),
    /** 조회가 진행 중인지. 버튼 비활성/진행표시에 사용. */
    val isBusy: Boolean = false,
) : UiState
