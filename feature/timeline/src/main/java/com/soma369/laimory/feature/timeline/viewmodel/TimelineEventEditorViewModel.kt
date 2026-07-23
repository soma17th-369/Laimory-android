package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.exception.TimelineEventUpdateException
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventPhotoAddition
import com.soma369.laimory.core.domain.model.timeline.TimelineEventUpdateField
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.usecase.ObserveTimelineRecordUseCase
import com.soma369.laimory.core.domain.usecase.UpdateTimelineEventUseCase
import com.soma369.laimory.core.domain.usecase.UploadTimelineEventPhotoUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorForm
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiContent
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiSideEffect
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiState
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorValidation
import com.soma369.laimory.feature.timeline.state.TimelineEventPendingPhoto
import com.soma369.laimory.feature.timeline.state.TimelineEventPhotoUploadState
import com.soma369.laimory.feature.timeline.state.TimelineEventTimeField
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TimelineEventEditorViewModel
    @Inject
    constructor(
        private val observeTimelineRecordUseCase: ObserveTimelineRecordUseCase,
        private val uploadTimelineEventPhotoUseCase: UploadTimelineEventPhotoUseCase,
        private val updateTimelineEventUseCase: UpdateTimelineEventUseCase,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<TimelineEventEditorUiState, TimelineEventEditorUiIntent, TimelineEventEditorUiSideEffect>(
            TimelineEventEditorUiState(),
        ) {
        override suspend fun handleIntent(intent: TimelineEventEditorUiIntent) {
            when (intent) {
                is TimelineEventEditorUiIntent.Initialize -> initialize(intent.timelineEventId)
                is TimelineEventEditorUiIntent.ChangeEventType ->
                    changeForm(transform = { copy(eventType = intent.eventType) })
                is TimelineEventEditorUiIntent.ChangeTitle ->
                    changeForm(
                        transform = { copy(title = intent.value) },
                        clearValidation = { afterTitleChanged(intent.value) },
                    )
                is TimelineEventEditorUiIntent.ChangeSubtitle ->
                    changeForm(
                        transform = { copy(subtitle = intent.value) },
                        clearValidation = { copy(subtitleError = null) },
                    )
                is TimelineEventEditorUiIntent.ChangeMemo ->
                    changeForm(
                        transform = { copy(memo = intent.value) },
                        clearValidation = { copy(memoError = null) },
                    )
                is TimelineEventEditorUiIntent.ShowTimePicker -> showTimePicker(intent.field)
                TimelineEventEditorUiIntent.DismissTimePicker ->
                    updateState { copy(editingTimeField = null) }
                is TimelineEventEditorUiIntent.SelectTime -> selectTime(intent.field, intent.time)
                TimelineEventEditorUiIntent.ClearEndTime -> clearEndTime()
                is TimelineEventEditorUiIntent.AddPhotos -> addPhotos(intent.clientPhotoUris)
                is TimelineEventEditorUiIntent.RemovePendingPhoto -> removePendingPhoto(intent.rawId)
                TimelineEventEditorUiIntent.OpenPhotoPicker ->
                    if (canEdit()) sendEffect(TimelineEventEditorUiSideEffect.LaunchPhotoPicker)
                TimelineEventEditorUiIntent.Save -> save()
                TimelineEventEditorUiIntent.NavigateBack -> navigateBack()
                TimelineEventEditorUiIntent.ConfirmDiscard -> discardAndNavigateBack()
                TimelineEventEditorUiIntent.DismissDiscard ->
                    updateState { copy(isDiscardDialogVisible = false) }
                TimelineEventEditorUiIntent.RequestDelete -> requestDelete()
            }
        }

        private fun initialize(timelineEventId: Long) {
            if (state.value.timelineEventId == timelineEventId && state.value.form != null) return
            val event =
                observeTimelineRecordUseCase()
                    .value
                    ?.events
                    ?.firstOrNull { it.timelineEventId == timelineEventId }
            if (event == null) {
                updateState {
                    copy(
                        timelineEventId = timelineEventId,
                        content = TimelineEventEditorUiContent.Unavailable,
                    )
                }
                return
            }

            val form = event.toEditorForm()
            updateState {
                TimelineEventEditorUiState(
                    timelineEventId = timelineEventId,
                    content = TimelineEventEditorUiContent.Editor,
                    originalForm = form,
                    form = form,
                    existingPhotoUrls = event.existingPhotoUrls(),
                )
            }
        }

        private fun changeForm(
            transform: TimelineEventEditorForm.() -> TimelineEventEditorForm,
            clearValidation: TimelineEventEditorValidation.() -> TimelineEventEditorValidation = { this },
        ) {
            if (!canEdit()) return
            updateState {
                copy(
                    form = form?.transform(),
                    validation = validation.clearValidation(),
                )
            }
        }

        private fun showTimePicker(field: TimelineEventTimeField) {
            if (!canEdit()) return
            updateState { copy(editingTimeField = field) }
        }

        private fun selectTime(
            field: TimelineEventTimeField,
            time: LocalTime,
        ) {
            if (!canEdit()) return
            updateState {
                val currentForm = form ?: return@updateState this
                val normalizedTime = time.withSecond(0).withNano(0)
                val nextForm =
                    when (field) {
                        TimelineEventTimeField.START ->
                            currentForm.copy(startAt = currentForm.startAt.with(normalizedTime))
                        TimelineEventTimeField.END ->
                            currentForm.copy(
                                endAt =
                                    (currentForm.endAt ?: currentForm.startAt)
                                        .with(normalizedTime)
                                        .let { selectedEndAt ->
                                            if (selectedEndAt < currentForm.startAt) {
                                                selectedEndAt.plusDays(1)
                                            } else {
                                                selectedEndAt
                                            }
                                        },
                            )
                    }
                copy(
                    form = nextForm,
                    editingTimeField = null,
                    validation = validation.copy(timeError = null),
                )
            }
        }

        private fun clearEndTime() {
            if (!canEdit()) return
            updateState {
                copy(
                    form = form?.copy(endAt = null),
                    editingTimeField = null,
                    validation = validation.copy(timeError = null),
                )
            }
        }

        private fun addPhotos(clientPhotoUris: List<String>) {
            if (!canEdit() || clientPhotoUris.isEmpty()) return
            updateState {
                val knownUris = pendingPhotos.mapTo(mutableSetOf()) { it.clientPhotoUri }
                val additions =
                    clientPhotoUris
                        .distinct()
                        .filterNot(knownUris::contains)
                        .map { uri ->
                            TimelineEventPendingPhoto(
                                rawId = UUID.randomUUID().toString(),
                                clientPhotoUri = uri,
                            )
                        }
                copy(pendingPhotos = pendingPhotos + additions)
            }
        }

        private fun removePendingPhoto(rawId: String) {
            if (!canEdit()) return
            updateState { copy(pendingPhotos = pendingPhotos.filterNot { it.rawId == rawId }) }
        }

        private suspend fun save() {
            val current = state.value
            if (current.isSaving || current.isReadOnly) return
            if (!current.hasUnsavedChanges) return
            val form = current.form ?: return
            val validation = form.validate()
            updateState { copy(validation = validation) }
            if (!validation.isValid) {
                if (validation.titleError != null) {
                    sendEffect(TimelineEventEditorUiSideEffect.FocusTitle)
                }
                return
            }

            updateState { copy(isSaving = true) }
            if (!uploadPendingPhotos()) {
                updateState { copy(isSaving = false) }
                return
            }

            val readyState = state.value
            val readyForm =
                readyState.form ?: run {
                    updateState { copy(isSaving = false) }
                    return
                }
            val command = readyState.toUpdateCommand(readyForm)
            updateTimelineEventUseCase(command)
                .onSuccess {
                    clearEditorState()
                    navigationHelper.navigateToBack()
                }.onFailure(::handleUpdateFailure)
        }

        private suspend fun uploadPendingPhotos(): Boolean {
            state.value.pendingPhotos.forEach { pending ->
                if (pending.uploadedFilename != null) return@forEach
                updatePhoto(pending.rawId) {
                    copy(uploadState = TimelineEventPhotoUploadState.UPLOADING)
                }
                val result = uploadTimelineEventPhotoUseCase(pending.clientPhotoUri)
                val filename = result.getOrNull()
                if (filename == null) {
                    updatePhoto(pending.rawId) {
                        copy(uploadState = TimelineEventPhotoUploadState.FAILED)
                    }
                    result.exceptionOrNull()?.let(::handleFailure)
                    return false
                }
                updatePhoto(pending.rawId) {
                    copy(
                        uploadState = TimelineEventPhotoUploadState.UPLOADED,
                        uploadedFilename = filename,
                    )
                }
            }
            return true
        }

        private fun updatePhoto(
            rawId: String,
            transform: TimelineEventPendingPhoto.() -> TimelineEventPendingPhoto,
        ) {
            updateState {
                copy(
                    pendingPhotos =
                        pendingPhotos.map { photo ->
                            if (photo.rawId == rawId) photo.transform() else photo
                        },
                )
            }
        }

        private fun handleUpdateFailure(error: Throwable) {
            updateState { copy(isSaving = false) }
            val reason = (error as? TimelineEventUpdateException)?.reason
            when (reason) {
                TimelineEventUpdateException.Reason.INVALID_REQUEST ->
                    sendEffect(TimelineEventEditorUiSideEffect.ShowSnackbar("입력값을 다시 확인해 주세요."))
                TimelineEventUpdateException.Reason.PHOTO_LIMIT_EXCEEDED ->
                    sendEffect(TimelineEventEditorUiSideEffect.ShowSnackbar("추가할 수 있는 사진 수를 초과했어요."))
                TimelineEventUpdateException.Reason.EVENT_UNAVAILABLE ->
                    updateState { copy(content = TimelineEventEditorUiContent.Unavailable) }
                TimelineEventUpdateException.Reason.RECORD_ALREADY_SAVED -> {
                    updateState { copy(isReadOnly = true) }
                    sendEffect(TimelineEventEditorUiSideEffect.ShowSnackbar("작성 완료된 기록은 수정할 수 없어요."))
                }
                TimelineEventUpdateException.Reason.DATE_OPERATION_IN_PROGRESS ->
                    sendEffect(TimelineEventEditorUiSideEffect.ShowSnackbar("같은 날짜의 작업이 진행 중이에요. 잠시 후 다시 시도해 주세요."))
                null -> handleFailure(error)
            }
        }

        private fun navigateBack() {
            if (state.value.isSaving) return
            if (state.value.hasUnsavedChanges) {
                updateState { copy(isDiscardDialogVisible = true) }
            } else {
                navigationHelper.navigateToBack()
            }
        }

        private fun discardAndNavigateBack() {
            clearEditorState()
            navigationHelper.navigateToBack()
        }

        private fun clearEditorState() {
            updateState { TimelineEventEditorUiState() }
        }

        private fun requestDelete() {
            val timelineEventId = state.value.timelineEventId ?: return
            if (state.value.isSaving) return
            sendEffect(TimelineEventEditorUiSideEffect.RequestDelete(timelineEventId))
        }

        private fun canEdit(): Boolean =
            with(state.value) {
                content == TimelineEventEditorUiContent.Editor && !isSaving && !isReadOnly
            }

        private fun TimelineEventEditorUiState.toUpdateCommand(form: TimelineEventEditorForm): UpdateTimelineEventCommand {
            val original = requireNotNull(originalForm)
            val photoAdditions =
                pendingPhotos.map { photo ->
                    TimelineEventPhotoAddition(
                        rawId = photo.rawId,
                        startAt = null,
                        endAt = null,
                        filename = requireNotNull(photo.uploadedFilename),
                        clientPhotoUri = photo.clientPhotoUri,
                        latitude = null,
                        longitude = null,
                    )
                }
            return UpdateTimelineEventCommand(
                timelineEventId = requireNotNull(timelineEventId),
                title = form.title.trim(),
                subtitle = form.subtitle.trim().ifBlank { null },
                startAt = form.startAt,
                endAt = form.endAt,
                eventType = form.eventType.takeIf { it != original.eventType },
                memo =
                    if (form.memo == original.memo) {
                        TimelineEventUpdateField.Unchanged
                    } else {
                        TimelineEventUpdateField.Value(form.memo.takeUnless(String::isBlank))
                    },
                photosToAdd =
                    if (photoAdditions.isEmpty()) {
                        TimelineEventUpdateField.Unchanged
                    } else {
                        TimelineEventUpdateField.Value(photoAdditions)
                    },
            )
        }
    }

private fun TimelineEvent.toEditorForm() =
    TimelineEventEditorForm(
        eventType = eventType,
        title = title,
        subtitle = subtitle.orEmpty(),
        startAt = startAt,
        endAt = endAt,
        memo = memo.orEmpty(),
    )

private fun TimelineEvent.existingPhotoUrls(): List<String> =
    items
        .asSequence()
        .filter { it.itemType == TimelineItemType.PHOTO }
        .mapNotNull { it.photoUrl }
        .toList()

private fun TimelineEventEditorForm.validate() =
    TimelineEventEditorValidation(
        titleError =
            when (title.trim().length) {
                0 -> "제목을 입력해 주세요."
                in 1..MAX_SHORT_TEXT_LENGTH -> null
                else -> "제목은 ${MAX_SHORT_TEXT_LENGTH}자까지 입력할 수 있어요."
            },
        subtitleError =
            if (subtitle.trim().length > MAX_SHORT_TEXT_LENGTH) {
                "설명은 ${MAX_SHORT_TEXT_LENGTH}자까지 입력할 수 있어요."
            } else {
                null
            },
        timeError =
            if (endAt != null && endAt < startAt) {
                "종료 시각은 시작 시각보다 빠를 수 없어요."
            } else {
                null
            },
        memoError =
            if (memo.length > MAX_MEMO_LENGTH) {
                "메모는 ${MAX_MEMO_LENGTH}자까지 입력할 수 있어요."
            } else {
                null
            },
    )

private fun TimelineEventEditorValidation.afterTitleChanged(value: String): TimelineEventEditorValidation =
    copy(
        titleError =
            when {
                value.isBlank() -> "제목을 입력해 주세요."
                titleError != null && value.trim().length > MAX_SHORT_TEXT_LENGTH ->
                    "제목은 ${MAX_SHORT_TEXT_LENGTH}자까지 입력할 수 있어요."
                else -> null
            },
    )

private const val MAX_SHORT_TEXT_LENGTH = 255
private const val MAX_MEMO_LENGTH = 10_000
