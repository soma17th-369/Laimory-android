package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalDate
import java.time.LocalTime

sealed interface HomeUiIntent : UiIntent {
    data object NavigateToCollection : HomeUiIntent

    data object OpenDraftSheet : HomeUiIntent

    data object DismissDraftSheet : HomeUiIntent

    data object OpenPhotoSheet : HomeUiIntent

    data object DismissPhotoSheet : HomeUiIntent

    data class TogglePhoto(
        val rawId: String,
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
}
