package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalDate

sealed interface TimelineUiIntent : UiIntent {
    data class SelectDate(val date: LocalDate) : TimelineUiIntent

    data object ShowDatePicker : TimelineUiIntent

    data object DismissDatePicker : TimelineUiIntent

    data class RequestUpload(val target: UploadTarget) : TimelineUiIntent

    data object ConfirmUpload : TimelineUiIntent

    data object DismissUpload : TimelineUiIntent
}
