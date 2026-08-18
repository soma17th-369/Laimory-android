package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalDate
import java.time.LocalTime

sealed interface HomeUiIntent : UiIntent {
    data object NavigateToCollection : HomeUiIntent

    data object OpenDraftSheet : HomeUiIntent

    data object DismissDraftSheet : HomeUiIntent

    data object OpenPhotoSheet : HomeUiIntent

    data object RequestAdditionalPhotoAccess : HomeUiIntent

    data class ResolvePhotoAccess(
        val granted: Boolean,
        val limited: Boolean = false,
    ) : HomeUiIntent

    data class RefreshPhotos(
        val hasAccess: Boolean,
        val limited: Boolean = false,
    ) : HomeUiIntent

    data object DismissPhotoSheet : HomeUiIntent

    data class TogglePhoto(
        val mediaStoreId: Long,
    ) : HomeUiIntent

    data class TogglePhotoDate(
        val date: LocalDate,
    ) : HomeUiIntent

    data object ToggleAllPhotos : HomeUiIntent

    data object ConfirmPhotoSelection : HomeUiIntent

    data object ShowDatePicker : HomeUiIntent

    data object DismissDatePicker : HomeUiIntent

    data class SelectDate(
        val date: LocalDate,
    ) : HomeUiIntent

    /** 시각 선택 시트를 열고 누른 줄을 펼친다. */
    data class ShowTimePicker(
        val field: HomeTimeField,
    ) : HomeUiIntent

    /** 시트 안에서 펼친 줄을 바꾼다. null 이면 모두 접는다. */
    data class ExpandTimeField(
        val field: HomeTimeField?,
    ) : HomeUiIntent

    /**
     * 시트 롤러를 굴린 결과. 확인 전까지는 시트 임시 값만 바뀐다.
     *
     * 종료 줄의 [date]는 날짜 롤러가 가리키는 날이라 당일·익일 선택을 겸한다.
     */
    data class ChangeSheetTime(
        val field: HomeTimeField,
        val date: LocalDate,
        val time: LocalTime,
    ) : HomeUiIntent

    /** 시트의 확인 — 임시 값을 기록 범위로 확정한다. */
    data object ConfirmTimeSheet : HomeUiIntent

    data object DismissTimePicker : HomeUiIntent

    /** 전송 스냅샷을 확정하고 데이터 전송 동의 화면으로 이동한다. 생성 API 는 동의 완료 후에만 호출된다. */
    data object CreateDraft : HomeUiIntent

    /** 동의 화면에서 제출을 마치고 복귀했는지 확인한다. 홈 재진입(ON_RESUME)마다 1회 소비한다. */
    data object ConsumeDraftConsentResult : HomeUiIntent

    data object RetryDraft : HomeUiIntent

    data object ContinueWaiting : HomeUiIntent

    data object StartNewDraft : HomeUiIntent

    data object ViewDraft : HomeUiIntent

    /** 홈 진입·복귀·재시도 시 지난 기록 목록을 서버와 동기화한다. */
    data object SyncPastRecords : HomeUiIntent

    data class SelectPastRecord(
        val recordDate: LocalDate,
    ) : HomeUiIntent
}
