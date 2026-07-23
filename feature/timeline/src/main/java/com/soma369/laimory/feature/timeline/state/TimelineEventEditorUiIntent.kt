package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalTime

sealed interface TimelineEventEditorUiIntent : UiIntent {
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

    data class ShowTimePicker(
        val field: TimelineEventTimeField,
    ) : TimelineEventEditorUiIntent

    data object DismissTimePicker : TimelineEventEditorUiIntent

    data class SelectTime(
        val field: TimelineEventTimeField,
        val time: LocalTime,
    ) : TimelineEventEditorUiIntent

    data object ClearEndTime : TimelineEventEditorUiIntent

    data class AddPhotos(
        val clientPhotoUris: List<String>,
    ) : TimelineEventEditorUiIntent

    data class RemovePendingPhoto(
        val rawId: String,
    ) : TimelineEventEditorUiIntent

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
