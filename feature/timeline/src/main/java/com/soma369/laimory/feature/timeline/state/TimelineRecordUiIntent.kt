package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalDate

sealed interface TimelineRecordUiIntent : UiIntent {
    data class Initialize(
        val recordDate: LocalDate?,
    ) : TimelineRecordUiIntent

    data object RetryLoad : TimelineRecordUiIntent

    data object NavigateBack : TimelineRecordUiIntent

    data object RequestSave : TimelineRecordUiIntent

    data object ConfirmSave : TimelineRecordUiIntent

    data object DismissSave : TimelineRecordUiIntent

    data object RequestDelete : TimelineRecordUiIntent

    data object ConfirmDelete : TimelineRecordUiIntent

    data object DismissDelete : TimelineRecordUiIntent

    data object FinishDelete : TimelineRecordUiIntent

    data class SelectEvent(
        val timelineEventId: Long,
    ) : TimelineRecordUiIntent

    data class EditMemo(
        val timelineEventId: Long,
    ) : TimelineRecordUiIntent

    data class ChangeMemo(
        val value: String,
    ) : TimelineRecordUiIntent

    data object CancelMemoEdit : TimelineRecordUiIntent

    data object ConfirmMemoEdit : TimelineRecordUiIntent
}
