package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState
import com.soma369.laimory.feature.timeline.model.TimelineRecordUiModel

@Immutable
data class TimelineRecordUiState(
    val content: TimelineRecordUiContent = TimelineRecordUiContent.Loading,
) : UiState

@Immutable
sealed interface TimelineRecordUiContent {
    data object Loading : TimelineRecordUiContent

    /** 프로세스 종료 등으로 폴링 SUCCESS 결과가 현재 상태 홀더에 없는 경우. */
    data object Unavailable : TimelineRecordUiContent

    data class Record(
        val value: TimelineRecordUiModel,
    ) : TimelineRecordUiContent
}
