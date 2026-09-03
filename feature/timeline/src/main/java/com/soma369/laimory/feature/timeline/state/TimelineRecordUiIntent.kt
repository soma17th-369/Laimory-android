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

    /** 플로팅 버튼으로 새 이벤트를 만든다. 편집 모드에서만 연다. */
    data object AddEvent : TimelineRecordUiIntent

    /** 앱바 감정을 눌러 수정 시트를 연다. 저장된 기록의 편집 모드에서만 열린다. */
    data object EditEmotion : TimelineRecordUiIntent

    /** 시트의 확인 — 시트가 열린 목적에 따라 작성 완료 또는 감정 교체를 요청한다. */
    data object ConfirmEmotion : TimelineRecordUiIntent

    data object DismissEmotionSheet : TimelineRecordUiIntent

    /** 읽기 모드 `⋮` 메뉴의 `편집하기`. 서버 상태는 바꾸지 않는다. */
    data object EnterEditMode : TimelineRecordUiIntent

    /** 편집 모드 상단 `X`. 저장이 아니라 화면 모드만 닫는다. */
    data object ExitEditMode : TimelineRecordUiIntent

    data object RequestDelete : TimelineRecordUiIntent

    data object ConfirmDelete : TimelineRecordUiIntent

    data object DismissDelete : TimelineRecordUiIntent

    data object FinishDelete : TimelineRecordUiIntent

    /** 카드의 휴지통. 편집 모드에서만 받는다. */
    data class RequestEventDelete(
        val timelineEventId: Long,
    ) : TimelineRecordUiIntent

    data object ConfirmEventDelete : TimelineRecordUiIntent

    data object DismissEventDelete : TimelineRecordUiIntent

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
