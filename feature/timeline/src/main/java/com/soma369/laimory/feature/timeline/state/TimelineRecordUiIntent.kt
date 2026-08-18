package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalDate

sealed interface TimelineRecordUiIntent : UiIntent {
    data class Initialize(
        val recordDate: LocalDate?,
    ) : TimelineRecordUiIntent

    data object RetryLoad : TimelineRecordUiIntent

    data object NavigateBack : TimelineRecordUiIntent

    /** 저장 CTA — 곧바로 저장하지 않고 감정 선택 시트를 연다. */
    data object RequestSave : TimelineRecordUiIntent

    data class SelectEmotion(
        val emotion: TimelineEmotion,
    ) : TimelineRecordUiIntent

    /** 시트의 `확인` — 선택한 감정으로 실제 저장을 요청한다. */
    data object ConfirmEmotion : TimelineRecordUiIntent

    data object DismissEmotionSheet : TimelineRecordUiIntent

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
