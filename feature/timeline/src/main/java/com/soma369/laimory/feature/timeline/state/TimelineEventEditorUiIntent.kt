package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.ui.base.UiIntent
import com.soma369.laimory.core.ui.component.timepicker.TimePickerColumn
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface TimelineEventEditorUiIntent : UiIntent {
    /** 새 이벤트를 만들기 위해 빈 편집기를 연다. */
    data class InitializeNew(
        val recordDate: LocalDate,
    ) : TimelineEventEditorUiIntent

    data class Initialize(
        val timelineEventId: Long,
    ) : TimelineEventEditorUiIntent

    data class ChangeEventType(
        val eventType: TimelineEventType,
    ) : TimelineEventEditorUiIntent

    data class ChangeTitle(
        val value: String,
    ) : TimelineEventEditorUiIntent

    data class ChangeSubtitle(
        val value: String,
    ) : TimelineEventEditorUiIntent

    data class ChangeMemo(
        val value: String,
    ) : TimelineEventEditorUiIntent

    /** 시간 설정 시트를 열고 [field] 줄을 펼친다. 종료가 비어 있으면 편집할 기준 값을 채워 연다. */
    data class OpenTimeSheet(
        val field: TimelineEventTimeField,
    ) : TimelineEventEditorUiIntent

    /** 시트 안에서 펼칠 줄을 지정한다. null이면 모두 접는다 — 펼침은 한 번에 하나만 유지한다. */
    data class ExpandTimeField(
        val field: TimelineEventTimeField?,
    ) : TimelineEventEditorUiIntent

    /**
     * 시트가 고른 날짜·시각을 임시 값에 반영한다. 폼에는 확인을 눌러야 옮겨 담는다.
     *
     * [column]은 사용자가 방금 굴린 열이다 — 시작·종료가 뒤집히면 그 열의 단위로 되돌린다.
     */
    data class ChangeTime(
        val field: TimelineEventTimeField,
        val dateTime: LocalDateTime,
        val column: TimePickerColumn,
    ) : TimelineEventEditorUiIntent

    /** 시트의 임시 값을 폼에 확정하고 시트를 닫는다. */
    data object ConfirmTimeSheet : TimelineEventEditorUiIntent

    /** 시트를 닫고 임시 값을 버린다. */
    data object DismissTimeSheet : TimelineEventEditorUiIntent

    data object ClearEndTime : TimelineEventEditorUiIntent

    data class AddPhotos(
        val clientPhotoUris: List<String>,
    ) : TimelineEventEditorUiIntent

    data class RemovePendingPhoto(
        val rawId: String,
    ) : TimelineEventEditorUiIntent

    data class RequestExistingPhotoRemoval(
        val timelineItemId: Long,
    ) : TimelineEventEditorUiIntent

    data object ConfirmExistingPhotoRemoval : TimelineEventEditorUiIntent

    data object DismissExistingPhotoRemoval : TimelineEventEditorUiIntent

    data object OpenPhotoPicker : TimelineEventEditorUiIntent

    data object Save : TimelineEventEditorUiIntent

    data object NavigateBack : TimelineEventEditorUiIntent

    data object ConfirmDiscard : TimelineEventEditorUiIntent

    data object DismissDiscard : TimelineEventEditorUiIntent

    data object RequestDelete : TimelineEventEditorUiIntent

    data object ConfirmDelete : TimelineEventEditorUiIntent

    data object DismissDelete : TimelineEventEditorUiIntent

    data object FinishDelete : TimelineEventEditorUiIntent
}
