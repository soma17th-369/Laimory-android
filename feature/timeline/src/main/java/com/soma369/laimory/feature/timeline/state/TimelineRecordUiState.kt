package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.timeline.TimelineEventMemoPolicy
import com.soma369.laimory.core.ui.base.UiState
import com.soma369.laimory.feature.timeline.model.TimelineRecordUiModel
import java.time.LocalDate

@Immutable
data class TimelineRecordUiState(
    val content: TimelineRecordUiContent = TimelineRecordUiContent.Loading,
    val memoEditor: TimelineMemoEditorState? = null,
    val deleteTarget: TimelineRecordDeleteTarget? = null,
    val deleteDialogState: TimelineDeleteDialogState = TimelineDeleteDialogState.Hidden,
    val emotionSheet: TimelineEmotionSheetState? = null,
    val isSavingRecord: Boolean = false,
) : UiState {
    val isDeleting: Boolean
        get() = deleteDialogState == TimelineDeleteDialogState.Deleting
}

@Immutable
data class TimelineMemoEditorState(
    val timelineEventId: Long,
    val originalMemo: String,
    val draftMemo: String,
    val isSaving: Boolean = false,
) {
    val hasChanges: Boolean
        get() = draftMemo != originalMemo

    val isValid: Boolean
        get() = draftMemo.length <= TimelineEventMemoPolicy.MAX_LENGTH

    val isConfirmEnabled: Boolean
        get() = hasChanges && isValid && !isSaving
}

@Immutable
data class TimelineRecordDeleteTarget(
    val recordDate: LocalDate,
)

@Immutable
sealed interface TimelineRecordUiContent {
    data object Loading : TimelineRecordUiContent

    /** 단건 조회 `-404` 등으로 기록이 이미 삭제됐거나 접근할 수 없는 경우. */
    data object Unavailable : TimelineRecordUiContent

    /** 네트워크 오류 등으로 단건 조회에 실패한 경우. 다시 시도할 수 있다. */
    data object LoadFailed : TimelineRecordUiContent

    data class Record(
        val value: TimelineRecordUiModel,
    ) : TimelineRecordUiContent
}
