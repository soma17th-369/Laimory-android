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

    data class ShowTimePicker(
        val field: HomeTimeField,
    ) : HomeUiIntent

    data object DismissTimePicker : HomeUiIntent

    data class SelectTime(
        val field: HomeTimeField,
        val time: LocalTime,
    ) : HomeUiIntent

    data class SelectEndDay(
        val endDay: DraftEndDay,
    ) : HomeUiIntent

    data object CreateDraft : HomeUiIntent

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
