package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.ui.base.UiState
import com.soma369.laimory.core.ui.component.timepicker.TimePickerColumn
import java.time.LocalDate
import java.time.LocalDateTime

@Immutable
data class TimelineEventEditorUiState(
    val timelineEventId: Long? = null,
    /** 이 이벤트가 속한 기록 날짜. 시각 선택의 `당일`·`익일` 기준이 된다. */
    val recordDate: LocalDate? = null,
    val content: TimelineEventEditorUiContent = TimelineEventEditorUiContent.Loading,
    val originalForm: TimelineEventEditorForm? = null,
    val form: TimelineEventEditorForm? = null,
    val existingPhotos: List<TimelineEventExistingPhoto> = emptyList(),
    val pendingPhotos: List<TimelineEventPendingPhoto> = emptyList(),
    val validation: TimelineEventEditorValidation = TimelineEventEditorValidation(),
    val timeSheet: TimelineEventTimeSheetState? = null,
    val isSaving: Boolean = false,
    val isReadOnly: Boolean = false,
    val isDiscardDialogVisible: Boolean = false,
    val deleteDialogState: TimelineDeleteDialogState = TimelineDeleteDialogState.Hidden,
    val photoDeleteDialogState: TimelineEventPhotoDeleteDialogState = TimelineEventPhotoDeleteDialogState.Hidden,
) : UiState {
    val hasUnsavedChanges: Boolean
        get() = originalForm != null && (form != originalForm || pendingPhotos.isNotEmpty())

    val isSaveEnabled: Boolean
        get() =
            content == TimelineEventEditorUiContent.Editor &&
                form?.title?.isNotBlank() == true &&
                hasUnsavedChanges &&
                !isSaving &&
                !isReadOnly &&
                deleteDialogState == TimelineDeleteDialogState.Hidden &&
                photoDeleteDialogState == TimelineEventPhotoDeleteDialogState.Hidden

    val isDeleting: Boolean
        get() = deleteDialogState == TimelineDeleteDialogState.Deleting

    val isDeletingPhoto: Boolean
        get() = photoDeleteDialogState is TimelineEventPhotoDeleteDialogState.Deleting
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

@Immutable
data class TimelineEventExistingPhoto(
    val timelineItemId: Long,
    val photoUrl: String?,
)

@Immutable
sealed interface TimelineEventPhotoDeleteDialogState {
    data object Hidden : TimelineEventPhotoDeleteDialogState

    data class Confirmation(
        val photo: TimelineEventExistingPhoto,
    ) : TimelineEventPhotoDeleteDialogState

    data class Deleting(
        val photo: TimelineEventExistingPhoto,
    ) : TimelineEventPhotoDeleteDialogState

    data class RetryableError(
        val photo: TimelineEventExistingPhoto,
        val message: String,
    ) : TimelineEventPhotoDeleteDialogState
}

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

/**
 * 시간 설정 시트가 편집 중인 임시 값.
 *
 * 시트는 확인을 눌러야 폼에 반영되므로, 여기서만 값을 굴리고 닫으면 그대로 버린다.
 * [expandedField]가 null이면 두 줄 모두 접힌 상태다.
 *
 * [baseDate]는 `당일`이 가리키는 날짜다. 값이 바뀔 때마다 다시 잡으면 같은 롤러 항목이 다른 날짜를
 * 뜻하게 되므로, 시트를 여는 시점의 기록 날짜로 고정한다.
 */
@Immutable
data class TimelineEventTimeSheetState(
    val baseDate: LocalDate,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    val expandedField: TimelineEventTimeField?,
) {
    /** 종료가 시작보다 뒤일 때만 확정할 수 있다. 종료를 두지 않는 이벤트는 언제나 확정 가능하다. */
    val isConfirmEnabled: Boolean
        get() = endAt == null || endAt.isAfter(startAt)

    /**
     * 시작·종료가 뒤집혔으면 종료를 최소 허용 시각으로 밀어낸다.
     *
     * 방금 굴린 열([column])이 고른 값은 살리고 나머지만 움직인다.
     * - 시를 굴렀으면 고른 **분을 그대로 두고** 시작을 갓 넘기는 시로만 올린다. 분이 시작 분보다
     *   크면 시작과 같은 시로 충분하고(09:30 시작에 08:40 → 09:40), 그렇지 않으면 한 시간 뒤다
     *   (09:30 시작에 08:10 → 10:10).
     * - 분·날짜를 굴렀으면 고른 분·날짜가 이미 범위를 벗어난 것이므로 최소 허용값(시작 +1분)에 붙인다.
     *
     * 분이 시작 분과 **같을 때도** 한 시간 올린다 — 길이가 0인 이벤트는 서버가 받지 않으므로
     * 종료는 시작과 같아도 안 되기 때문이다.
     *
     * 사용자가 방금 만진 쪽을 존중하고 반대쪽을 옮기므로, 시작을 종료 뒤로 올려도 같은 규칙이 적용된다.
     */
    fun withEndAfterStart(column: TimePickerColumn): TimelineEventTimeSheetState {
        val currentEnd = endAt ?: return this
        if (currentEnd.isAfter(startAt)) return this
        val pushed =
            when (column) {
                TimePickerColumn.HOUR -> {
                    val keptMinute = currentEnd.minute
                    val withKeptMinute = startAt.withMinute(keptMinute).withSecond(0).withNano(0)
                    if (keptMinute > startAt.minute) withKeptMinute else withKeptMinute.plusHours(1)
                }
                TimePickerColumn.MINUTE, TimePickerColumn.DATE -> startAt.plusMinutes(1)
            }
        return copy(endAt = pushed)
    }
}
