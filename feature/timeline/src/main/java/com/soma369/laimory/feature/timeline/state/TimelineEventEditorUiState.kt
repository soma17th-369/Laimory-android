package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.ui.base.UiState
import java.time.LocalDateTime

@Immutable
data class TimelineEventEditorUiState(
    val timelineEventId: Long? = null,
    val content: TimelineEventEditorUiContent = TimelineEventEditorUiContent.Loading,
    val originalForm: TimelineEventEditorForm? = null,
    val form: TimelineEventEditorForm? = null,
    val existingPhotoUrls: List<String> = emptyList(),
    val pendingPhotos: List<TimelineEventPendingPhoto> = emptyList(),
    val validation: TimelineEventEditorValidation = TimelineEventEditorValidation(),
    val editingTimeField: TimelineEventTimeField? = null,
    val isSaving: Boolean = false,
    val isReadOnly: Boolean = false,
    val isDiscardDialogVisible: Boolean = false,
) : UiState {
    val hasUnsavedChanges: Boolean
        get() = originalForm != null && (form != originalForm || pendingPhotos.isNotEmpty())

    val isSaveEnabled: Boolean
        get() =
            content == TimelineEventEditorUiContent.Editor &&
                form?.title?.isNotBlank() == true &&
                hasUnsavedChanges &&
                !isSaving &&
                !isReadOnly
}

@Immutable
sealed interface TimelineEventEditorUiContent {
    data object Loading : TimelineEventEditorUiContent

    data object Editor : TimelineEventEditorUiContent

    data object Unavailable : TimelineEventEditorUiContent
}

@Immutable
data class TimelineEventEditorForm(
    val eventType: TimelineEventType,
    val title: String,
    val subtitle: String,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    val memo: String,
)

@Immutable
data class TimelineEventPendingPhoto(
    val rawId: String,
    val clientPhotoUri: String,
    val uploadState: TimelineEventPhotoUploadState = TimelineEventPhotoUploadState.PENDING,
    val uploadedFilename: String? = null,
)

enum class TimelineEventPhotoUploadState {
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED,
}

@Immutable
data class TimelineEventEditorValidation(
    val titleError: String? = null,
    val subtitleError: String? = null,
    val timeError: String? = null,
    val memoError: String? = null,
) {
    val isValid: Boolean
        get() = titleError == null && subtitleError == null && timeError == null && memoError == null
}

enum class TimelineEventTimeField {
    START,
    END,
}
