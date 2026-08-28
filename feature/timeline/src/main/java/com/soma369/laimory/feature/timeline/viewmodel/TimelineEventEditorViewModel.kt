package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.exception.TimelineEventPhotoDeleteException
import com.soma369.laimory.core.domain.exception.TimelineEventUpdateException
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.timeline.CreateTimelineEventCommand
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventMemoPolicy
import com.soma369.laimory.core.domain.model.timeline.TimelineEventPhotoAddition
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineEventUpdateField
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.usecase.CreateTimelineEventUseCase
import com.soma369.laimory.core.domain.usecase.DeleteTimelineEventPhotoOutcome
import com.soma369.laimory.core.domain.usecase.DeleteTimelineEventPhotoUseCase
import com.soma369.laimory.core.domain.usecase.DeleteTimelineEventUseCase
import com.soma369.laimory.core.domain.usecase.ObserveTimelineRecordUseCase
import com.soma369.laimory.core.domain.usecase.UpdateTimelineEventUseCase
import com.soma369.laimory.core.domain.usecase.UploadTimelineEventPhotoUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.core.ui.component.timepicker.TimePickerColumn
import com.soma369.laimory.feature.timeline.state.TimelineDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorForm
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiContent
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiSideEffect
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiState
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorValidation
import com.soma369.laimory.feature.timeline.state.TimelineEventExistingPhoto
import com.soma369.laimory.feature.timeline.state.TimelineEventPendingPhoto
import com.soma369.laimory.feature.timeline.state.TimelineEventPhotoDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineEventPhotoUploadState
import com.soma369.laimory.feature.timeline.state.TimelineEventTimeField
import com.soma369.laimory.feature.timeline.state.TimelineEventTimeSheetState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TimelineEventEditorViewModel
    @Inject
    constructor(
        private val observeTimelineRecordUseCase: ObserveTimelineRecordUseCase,
        private val uploadTimelineEventPhotoUseCase: UploadTimelineEventPhotoUseCase,
        private val updateTimelineEventUseCase: UpdateTimelineEventUseCase,
        private val deleteTimelineEventUseCase: DeleteTimelineEventUseCase,
        private val deleteTimelineEventPhotoUseCase: DeleteTimelineEventPhotoUseCase,
        private val createTimelineEventUseCase: CreateTimelineEventUseCase,
        private val navigationHelper: NavigationHelper,
        private val clock: Clock,
    ) : BaseMviViewModel<TimelineEventEditorUiState, TimelineEventEditorUiIntent, TimelineEventEditorUiSideEffect>(
            TimelineEventEditorUiState(),
        ) {
        override suspend fun handleIntent(intent: TimelineEventEditorUiIntent) {
            when (intent) {
                is TimelineEventEditorUiIntent.InitializeNew -> initializeNew(intent.recordDate)
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
                is TimelineEventEditorUiIntent.OpenTimeSheet -> openTimeSheet(intent.field)
                is TimelineEventEditorUiIntent.ExpandTimeField -> expandTimeField(intent.field)
                is TimelineEventEditorUiIntent.ChangeTime ->
                    changeTime(intent.field, intent.dateTime, intent.column)
                TimelineEventEditorUiIntent.ConfirmTimeSheet -> confirmTimeSheet()
                TimelineEventEditorUiIntent.DismissTimeSheet -> updateState { copy(timeSheet = null) }
                TimelineEventEditorUiIntent.ClearEndTime -> clearEndTime()
                is TimelineEventEditorUiIntent.AddPhotos -> addPhotos(intent.clientPhotoUris)
                is TimelineEventEditorUiIntent.RemovePendingPhoto -> removePendingPhoto(intent.rawId)
                is TimelineEventEditorUiIntent.RequestExistingPhotoRemoval ->
                    requestExistingPhotoRemoval(intent.timelineItemId)
                TimelineEventEditorUiIntent.ConfirmExistingPhotoRemoval -> deleteExistingPhoto()
                TimelineEventEditorUiIntent.DismissExistingPhotoRemoval -> dismissExistingPhotoRemoval()
                TimelineEventEditorUiIntent.OpenPhotoPicker ->
                    if (canEdit()) sendEffect(TimelineEventEditorUiSideEffect.LaunchPhotoPicker)
                TimelineEventEditorUiIntent.Save -> save()
                TimelineEventEditorUiIntent.NavigateBack -> navigateBack()
                TimelineEventEditorUiIntent.ConfirmDiscard -> discardAndNavigateBack()
                TimelineEventEditorUiIntent.DismissDiscard ->
                    updateState { copy(isDiscardDialogVisible = false) }
                TimelineEventEditorUiIntent.RequestDelete -> requestDelete()
                TimelineEventEditorUiIntent.ConfirmDelete -> deleteEvent()
                TimelineEventEditorUiIntent.DismissDelete -> dismissDelete()
                TimelineEventEditorUiIntent.FinishDelete -> finishDelete()
            }
        }

        /**
         * 빈 편집기를 연다.
         *
         * 시작 시각은 **그 기록 날짜의 지금 시각**이다. `현재 일시`를 그대로 쓰면 8월 8일 기록에
         * 오늘 날짜가 박히고, `마지막 이벤트 뒤`로 밀면 하루 중간에 끼워 넣는 흔한 사용이 매번
         * 시각을 고쳐야 한다. 종료 시각은 비워 둔다 — 서버가 null 을 허용하고, 필요하면 사용자가
         * 넣는다.
         */
        private fun initializeNew(recordDate: LocalDate) {
            // 폼 유무만 보면 안 된다 — 이 앱의 NavDisplay 는 화면별 ViewModelStoreOwner 를 주지
            // 않아 같은 ViewModel 이 재사용된다. 기존 이벤트를 열었다가 바꾸지 않고 뒤로 나가면
            // 상태가 남아 있어, 그대로 두면 `+` 가 신규 대신 **이전 이벤트 편집**을 열고 저장이
            // POST 가 아니라 PATCH 로 나간다. 무엇을 위해 열렸는지까지 같아야 재사용한다.
            val current = state.value
            if (current.timelineEventId == null && current.recordDate == recordDate && current.form != null) return
            // 분 단위로 자른다. 피커가 분까지만 다루므로, 초·나노를 실어 보내면 화면에 보이는
            // 값과 저장된 값이 어긋나고 나중에 시각을 한 번만 고쳐도 초가 조용히 날아간다.
            val startAt =
                LocalDateTime.of(
                    recordDate,
                    LocalTime.now(clock.withZone(ZoneId.systemDefault())).truncatedTo(ChronoUnit.MINUTES),
                )
            val form =
                TimelineEventEditorForm(
                    eventType = TimelineEventType.UNKNOWN,
                    title = "",
                    subtitle = "",
                    startAt = startAt,
                    endAt = null,
                    memo = "",
                )
            updateState {
                TimelineEventEditorUiState(
                    timelineEventId = null,
                    recordDate = recordDate,
                    content = TimelineEventEditorUiContent.Editor,
                    originalForm = form,
                    form = form,
                )
            }
        }

        private fun initialize(timelineEventId: Long) {
            // 같은 이벤트를 다시 여는 경우만 상태를 재사용한다.
            if (state.value.timelineEventId == timelineEventId && state.value.form != null) return
            val timeline = observeTimelineRecordUseCase().value
            val event = timeline?.events?.firstOrNull { it.timelineEventId == timelineEventId }
            if (timeline == null || event == null) {
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
                    recordDate = timeline.recordDate,
                    content = TimelineEventEditorUiContent.Editor,
                    originalForm = form,
                    form = form,
                    existingPhotos = event.existingPhotos(),
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

        /**
         * 시간 설정 시트를 연다.
         *
         * 종료가 비어 있는데 종료 줄을 열면 롤러가 가리킬 값이 없으므로 기준 값을 채워 편집을 시작한다.
         * 이 값은 시트의 임시 값일 뿐이라 확인을 누르지 않으면 폼에 남지 않는다.
         */
        private fun openTimeSheet(field: TimelineEventTimeField) {
            if (!canEdit()) return
            updateState {
                val currentForm = form ?: return@updateState this
                val sheet =
                    TimelineEventTimeSheetState(
                        baseDate = recordDate ?: currentForm.startAt.toLocalDate(),
                        startAt = currentForm.startAt,
                        endAt = currentForm.endAt,
                        expandedField = field,
                    )
                copy(timeSheet = if (field == TimelineEventTimeField.END) sheet.withSeededEnd() else sheet)
            }
        }

        private fun expandTimeField(field: TimelineEventTimeField?) {
            updateState {
                val sheet = timeSheet ?: return@updateState this
                copy(timeSheet = sheet.copy(expandedField = field))
            }
        }

        /**
         * 롤러가 고른 날짜·시각을 시트의 임시 값에만 반영한다.
         *
         * 사용자가 종료 날짜를 직접 고르므로 시작보다 이른 종료를 익일로 추론하지 않는다. 다만 시작·종료가
         * 뒤집히면 종료를 최소 허용 시각으로 밀어 롤러가 따라 움직이게 한다. 서버에서 이미 뒤집힌 값이
         * 내려온 경우처럼 사용자가 만지지 않은 조합은 저장 시 [TimelineEventEditorValidation.timeError]가 막는다.
         */
        private fun changeTime(
            field: TimelineEventTimeField,
            dateTime: LocalDateTime,
            column: TimePickerColumn,
        ) {
            updateState {
                val sheet = timeSheet ?: return@updateState this
                val normalized = dateTime.withSecond(0).withNano(0)
                val nextSheet =
                    when (field) {
                        TimelineEventTimeField.START -> sheet.copy(startAt = normalized)
                        TimelineEventTimeField.END -> sheet.copy(endAt = normalized)
                    }
                copy(timeSheet = nextSheet.withEndAfterStart(column))
            }
        }

        private fun confirmTimeSheet() {
            updateState {
                val sheet = timeSheet ?: return@updateState this
                val currentForm = form ?: return@updateState this
                if (!sheet.isConfirmEnabled) return@updateState this
                copy(
                    form = currentForm.copy(startAt = sheet.startAt, endAt = sheet.endAt),
                    timeSheet = null,
                    validation = validation.copy(timeError = null),
                )
            }
        }

        private fun clearEndTime() {
            if (!canEdit()) return
            updateState {
                copy(
                    form = form?.copy(endAt = null),
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
            if (current.isSaving ||
                current.deleteDialogState != TimelineDeleteDialogState.Hidden ||
                current.photoDeleteDialogState != TimelineEventPhotoDeleteDialogState.Hidden
            ) {
                return
            }
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
            // 신규는 id 가 없다. 같은 화면이지만 서버 경로가 갈린다.
            val result =
                if (readyState.timelineEventId == null) {
                    createTimelineEventUseCase(readyState.toCreateCommand(readyForm)).map { }
                } else {
                    updateTimelineEventUseCase(readyState.toUpdateCommand(readyForm))
                }
            result
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
                TimelineEventUpdateException.Reason.DATE_OPERATION_IN_PROGRESS ->
                    sendEffect(TimelineEventEditorUiSideEffect.ShowSnackbar("같은 날짜의 작업이 진행 중이에요. 잠시 후 다시 시도해 주세요."))
                null -> handleFailure(error)
            }
        }

        private fun navigateBack() {
            if (state.value.isSaving || state.value.isDeleting || state.value.isDeletingPhoto) return
            // 시간 설정 시트가 열려 있으면 화면을 벗어나기 전에 시트부터 닫는다.
            if (state.value.timeSheet != null) {
                updateState { copy(timeSheet = null) }
                return
            }
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
            if (!canEdit() || state.value.timelineEventId == null) return
            updateState { copy(deleteDialogState = TimelineDeleteDialogState.Confirmation) }
        }

        private fun requestExistingPhotoRemoval(timelineItemId: Long) {
            if (!canEdit()) return
            val photo = state.value.existingPhotos.firstOrNull { it.timelineItemId == timelineItemId } ?: return
            updateState {
                copy(photoDeleteDialogState = TimelineEventPhotoDeleteDialogState.Confirmation(photo))
            }
        }

        private suspend fun deleteExistingPhoto() {
            val current = state.value
            val timelineEventId = current.timelineEventId ?: return
            val photo =
                when (val dialogState = current.photoDeleteDialogState) {
                    is TimelineEventPhotoDeleteDialogState.Confirmation -> dialogState.photo
                    is TimelineEventPhotoDeleteDialogState.RetryableError -> dialogState.photo
                    else -> return
                }
            updateState {
                copy(photoDeleteDialogState = TimelineEventPhotoDeleteDialogState.Deleting(photo))
            }
            deleteTimelineEventPhotoUseCase(timelineEventId, photo.timelineItemId)
                .onSuccess(::handlePhotoDeleteSuccess)
                .onFailure { handlePhotoDeleteFailure(photo, it) }
        }

        private fun handlePhotoDeleteSuccess(outcome: DeleteTimelineEventPhotoOutcome) {
            when (outcome) {
                DeleteTimelineEventPhotoOutcome.Deleted -> {
                    if (syncExistingPhotosFromSession()) {
                        updateState { copy(photoDeleteDialogState = TimelineEventPhotoDeleteDialogState.Hidden) }
                        sendEffect(TimelineEventEditorUiSideEffect.ShowSnackbar("사진을 이벤트에서 제거했어요."))
                    }
                }
                DeleteTimelineEventPhotoOutcome.Reconciled -> {
                    if (syncExistingPhotosFromSession()) {
                        updateState { copy(photoDeleteDialogState = TimelineEventPhotoDeleteDialogState.Hidden) }
                        sendEffect(TimelineEventEditorUiSideEffect.ShowSnackbar("사진 목록을 최신 상태로 갱신했어요."))
                    }
                }
                DeleteTimelineEventPhotoOutcome.EventUnavailable -> showUnavailableAfterPhotoDelete()
            }
        }

        private fun syncExistingPhotosFromSession(): Boolean {
            val timelineEventId = state.value.timelineEventId ?: return false
            val timeline = observeTimelineRecordUseCase().value
            val event = timeline?.events?.firstOrNull { it.timelineEventId == timelineEventId }
            if (timeline == null || event == null) {
                showUnavailableAfterPhotoDelete()
                return false
            }
            updateState { copy(existingPhotos = event.existingPhotos()) }
            return true
        }

        private fun showUnavailableAfterPhotoDelete() {
            updateState {
                copy(
                    content = TimelineEventEditorUiContent.Unavailable,
                    photoDeleteDialogState = TimelineEventPhotoDeleteDialogState.Hidden,
                )
            }
            sendEffect(TimelineEventEditorUiSideEffect.ShowSnackbar("이미 삭제됐거나 접근할 수 없는 이벤트예요."))
        }

        private fun handlePhotoDeleteFailure(
            photo: TimelineEventExistingPhoto,
            error: Throwable,
        ) {
            when (error) {
                is TimelineEventPhotoDeleteException ->
                    when (error.reason) {
                        TimelineEventPhotoDeleteException.Reason.ITEM_NOT_PHOTO ->
                            showRetryablePhotoDeleteError(photo, "사진을 제거하지 못했어요. 잠시 후 다시 시도해주세요.")
                    }
                is HandledException ->
                    updateState { copy(photoDeleteDialogState = TimelineEventPhotoDeleteDialogState.Hidden) }
                is ApiException.NetworkException ->
                    showRetryablePhotoDeleteError(photo, "네트워크 상태를 확인한 뒤 다시 시도해주세요.")
                else ->
                    showRetryablePhotoDeleteError(photo, "일시적인 오류예요. 잠시 후 다시 시도해주세요.")
            }
        }

        private fun showRetryablePhotoDeleteError(
            photo: TimelineEventExistingPhoto,
            message: String,
        ) {
            updateState {
                copy(
                    photoDeleteDialogState =
                        TimelineEventPhotoDeleteDialogState.RetryableError(
                            photo = photo,
                            message = message,
                        ),
                )
            }
        }

        private fun dismissExistingPhotoRemoval() {
            if (state.value.isDeletingPhoto) return
            updateState { copy(photoDeleteDialogState = TimelineEventPhotoDeleteDialogState.Hidden) }
        }

        private suspend fun deleteEvent() {
            val current = state.value
            val timelineEventId = current.timelineEventId ?: return
            if (current.deleteDialogState != TimelineDeleteDialogState.Confirmation &&
                current.deleteDialogState !is TimelineDeleteDialogState.RetryableError
            ) {
                return
            }

            updateState { copy(deleteDialogState = TimelineDeleteDialogState.Deleting) }
            deleteTimelineEventUseCase(timelineEventId)
                .onSuccess {
                    updateState { copy(deleteDialogState = TimelineDeleteDialogState.Success) }
                }.onFailure(::handleDeleteFailure)
        }

        private fun dismissDelete() {
            if (state.value.isDeleting) return
            updateState { copy(deleteDialogState = TimelineDeleteDialogState.Hidden) }
        }

        private fun finishDelete() {
            if (state.value.deleteDialogState != TimelineDeleteDialogState.Success) return
            clearEditorState()
            navigationHelper.navigateToBack()
        }

        private fun handleDeleteFailure(error: Throwable) {
            when (val action = error.toTimelineDeleteFailureAction()) {
                TimelineDeleteFailureAction.TargetUnavailable -> {
                    updateState {
                        copy(
                            content = TimelineEventEditorUiContent.Unavailable,
                            deleteDialogState = TimelineDeleteDialogState.Hidden,
                        )
                    }
                    sendEffect(TimelineEventEditorUiSideEffect.ShowSnackbar("이미 삭제됐거나 접근할 수 없는 이벤트예요."))
                }
                TimelineDeleteFailureAction.AlreadyHandled ->
                    updateState { copy(deleteDialogState = TimelineDeleteDialogState.Hidden) }
                is TimelineDeleteFailureAction.Retryable ->
                    showRetryableDeleteError(action.message)
            }
        }

        private fun showRetryableDeleteError(message: String) {
            updateState {
                copy(deleteDialogState = TimelineDeleteDialogState.RetryableError(message))
            }
        }

        private fun canEdit(): Boolean =
            with(state.value) {
                content == TimelineEventEditorUiContent.Editor &&
                    !isSaving &&
                    deleteDialogState == TimelineDeleteDialogState.Hidden &&
                    photoDeleteDialogState == TimelineEventPhotoDeleteDialogState.Hidden
            }

        /**
         * 신규 생성 명령.
         *
         * 수정과 달리 **바뀐 것만 골라 보내지 않는다** — 서버가 다섯 키를 모두 요구하므로 폼의
         * 현재 값을 그대로 싣는다.
         */
        private fun TimelineEventEditorUiState.toCreateCommand(form: TimelineEventEditorForm): CreateTimelineEventCommand =
            CreateTimelineEventCommand(
                recordDate = requireNotNull(recordDate),
                eventType = form.eventType,
                title = form.title.trim(),
                subtitle = form.subtitle.trim().ifBlank { null },
                startAt = form.startAt,
                endAt = form.endAt,
                memo = form.memo.trim().ifBlank { null },
                photosToAdd = photoAdditions(),
            )

        /** 업로드가 끝난 대기 사진을 서버 payload 로 옮긴다. 생성과 수정이 같은 목록을 쓴다. */
        private fun TimelineEventEditorUiState.photoAdditions(): List<TimelineEventPhotoAddition> =
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

        private fun TimelineEventEditorUiState.toUpdateCommand(form: TimelineEventEditorForm): UpdateTimelineEventCommand {
            val original = requireNotNull(originalForm)
            val photoAdditions = photoAdditions()
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

private fun TimelineEvent.existingPhotos(): List<TimelineEventExistingPhoto> =
    items
        .asSequence()
        .filter { it.itemType == TimelineItemType.PHOTO }
        .map {
            TimelineEventExistingPhoto(
                timelineItemId = it.timelineItemId,
                photoUrl = it.photoUrl,
            )
        }
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
            if (endAt != null && endAt <= startAt) {
                "종료 시각은 시작 시각보다 뒤여야 해요."
            } else {
                null
            },
        memoError =
            if (memo.length > TimelineEventMemoPolicy.MAX_LENGTH) {
                "메모는 ${String.format(Locale.getDefault(), "%,d", TimelineEventMemoPolicy.MAX_LENGTH)}자까지 " +
                    "입력할 수 있어요."
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
