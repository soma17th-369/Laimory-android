package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.exception.TimelineEventUpdateException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.DailyRecordReadOutcome
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.navigation.TimelineEventEditorPage
import com.soma369.laimory.core.domain.usecase.CompleteDailyRecordOutcome
import com.soma369.laimory.core.domain.usecase.CompleteDailyRecordUseCase
import com.soma369.laimory.core.domain.usecase.DeleteDailyRecordUseCase
import com.soma369.laimory.core.domain.usecase.GetDailyRecordUseCase
import com.soma369.laimory.core.domain.usecase.ObserveTimelineRecordUseCase
import com.soma369.laimory.core.domain.usecase.SaveTimelineRecordUseCase
import com.soma369.laimory.core.domain.usecase.UpdateTimelineEventMemoUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.timeline.model.toUiModel
import com.soma369.laimory.feature.timeline.state.TimelineDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineMemoEditorState
import com.soma369.laimory.feature.timeline.state.TimelineRecordDeleteTarget
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiContent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiSideEffect
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TimelineRecordViewModel
    @Inject
    constructor(
        observeTimelineRecordUseCase: ObserveTimelineRecordUseCase,
        private val getDailyRecordUseCase: GetDailyRecordUseCase,
        private val saveTimelineRecordUseCase: SaveTimelineRecordUseCase,
        private val completeDailyRecordUseCase: CompleteDailyRecordUseCase,
        private val updateTimelineEventMemoUseCase: UpdateTimelineEventMemoUseCase,
        private val deleteDailyRecordUseCase: DeleteDailyRecordUseCase,
        private val draftTaskCoordinator: DraftTaskCoordinator,
        private val navigationHelper: NavigationHelper,
        private val messageHelper: MessageHelper,
    ) : BaseMviViewModel<TimelineRecordUiState, TimelineRecordUiIntent, TimelineRecordUiSideEffect>(
            TimelineRecordUiState(),
        ) {
        private var requestedRecordDate: LocalDate? = null
        private var loadJob: Job? = null

        init {
            safeLaunch {
                observeTimelineRecordUseCase().collect { timeline ->
                    val requestedDate = requestedRecordDate ?: return@collect
                    updateState {
                        when {
                            timeline?.recordDate == requestedDate ->
                                copy(content = TimelineRecordUiContent.Record(timeline.toUiModel()))

                            // 표시 중이던 기록이 세션에서 사라진 경우(삭제 등).
                            timeline == null && content is TimelineRecordUiContent.Record ->
                                copy(content = TimelineRecordUiContent.Unavailable)

                            // 다른 기록을 가리키는 이전 세션은 무시한다.
                            else -> this
                        }
                    }
                }
            }
        }

        override suspend fun handleIntent(intent: TimelineRecordUiIntent) {
            when (intent) {
                is TimelineRecordUiIntent.Initialize -> initialize(intent.recordDate)
                TimelineRecordUiIntent.RetryLoad -> requestedRecordDate?.let(::loadRecord)
                TimelineRecordUiIntent.NavigateBack ->
                    navigateBack()
                TimelineRecordUiIntent.RequestSave -> saveRecord()
                TimelineRecordUiIntent.RequestDelete -> requestDelete()
                TimelineRecordUiIntent.ConfirmDelete -> deleteRecord()
                TimelineRecordUiIntent.DismissDelete -> dismissDelete()
                TimelineRecordUiIntent.FinishDelete -> finishDelete()
                is TimelineRecordUiIntent.SelectEvent ->
                    if (state.value.deleteDialogState == TimelineDeleteDialogState.Hidden &&
                        !state.value.isSavingRecord &&
                        state.value.memoEditor == null &&
                        state.value.editableRecord() != null
                    ) {
                        navigationHelper.navigateTo(TimelineEventEditorPage(intent.timelineEventId))
                    }
                is TimelineRecordUiIntent.EditMemo -> editMemo(intent.timelineEventId)
                is TimelineRecordUiIntent.ChangeMemo -> changeMemo(intent.value)
                TimelineRecordUiIntent.CancelMemoEdit -> cancelMemoEdit()
                TimelineRecordUiIntent.ConfirmMemoEdit -> updateMemo()
            }
        }

        private fun navigateBack() {
            val current = state.value
            if (current.isDeleting || current.isSavingRecord || current.memoEditor?.isSaving == true) return
            if (current.memoEditor != null) {
                updateState { copy(memoEditor = null) }
            } else {
                navigationHelper.navigateToBack()
            }
        }

        private fun initialize(recordDate: LocalDate?) {
            if (recordDate == null) {
                loadJob?.cancel()
                requestedRecordDate = null
                updateState { copy(content = TimelineRecordUiContent.Unavailable, memoEditor = null) }
                return
            }
            val isAlreadyPresented =
                requestedRecordDate == recordDate &&
                    state.value.content !is TimelineRecordUiContent.LoadFailed
            if (isAlreadyPresented) return
            requestedRecordDate = recordDate
            loadRecord(recordDate)
        }

        private fun loadRecord(recordDate: LocalDate) {
            // latest-wins: 새 기록 요청이 진행 중인 조회를 대체한다.
            loadJob?.cancel()
            loadJob =
                safeLaunch(
                    onError = { error ->
                        if (error !is CancellationException && requestedRecordDate == recordDate) {
                            updateState { copy(content = TimelineRecordUiContent.LoadFailed) }
                            handleFailure(error)
                        }
                    },
                ) {
                    updateState { copy(content = TimelineRecordUiContent.Loading, memoEditor = null) }
                    getDailyRecordUseCase(recordDate)
                        .onSuccess { outcome ->
                            if (requestedRecordDate != recordDate) return@onSuccess
                            when (outcome) {
                                is DailyRecordReadOutcome.Record -> {
                                    // 세션에 같은 값이 선저장돼 있으면 StateFlow가 재방출하지
                                    // 않으므로 화면 상태를 직접 전환한 뒤 세션에도 저장한다.
                                    saveTimelineRecordUseCase(outcome.value)
                                    updateState {
                                        copy(content = TimelineRecordUiContent.Record(outcome.value.toUiModel()))
                                    }
                                }
                                DailyRecordReadOutcome.Unavailable ->
                                    updateState { copy(content = TimelineRecordUiContent.Unavailable) }
                            }
                        }.onFailure { error ->
                            if (requestedRecordDate != recordDate) return@onFailure
                            updateState { copy(content = TimelineRecordUiContent.LoadFailed) }
                            handleFailure(error)
                        }
                }
        }

        /** 확인 다이얼로그 없이 즉시 작성 완료를 요청한다. 진행 중에는 CTA 로딩으로 중복을 막는다. */
        private suspend fun saveRecord() {
            val current = state.value
            val record = current.editableRecord() ?: return
            if (current.isSavingRecord ||
                current.deleteDialogState != TimelineDeleteDialogState.Hidden ||
                current.memoEditor != null
            ) {
                return
            }

            updateState { copy(isSavingRecord = true) }
            completeDailyRecordUseCase(record.recordDate)
                .onSuccess { outcome -> handleSaveOutcome(outcome, record.recordDate) }
                .onFailure(::handleSaveFailure)
        }

        private suspend fun handleSaveOutcome(
            outcome: CompleteDailyRecordOutcome,
            recordDate: LocalDate,
        ) {
            discardDraftTracking(recordDate)
            when (outcome) {
                CompleteDailyRecordOutcome.Completed,
                CompleteDailyRecordOutcome.AlreadySaved,
                -> {
                    // isSavingRecord는 화면 이탈까지 유지해 큐에 남은 중복 요청의 재실행을 막는다.
                    // 화면 side effect는 pop과 함께 수집이 끊길 수 있어 Root 수명 채널로 안내한다.
                    messageHelper.send(UserMessage.DailyRecordSaved)
                    navigationHelper.navigateToBack()
                }
                // 세션 정리로 화면은 Unavailable로 전환된다. 재시도가 가능해야 하므로 저장 중 상태는 푼다.
                CompleteDailyRecordOutcome.RecordUnavailable -> {
                    updateState { copy(isSavingRecord = false) }
                    sendEffect(TimelineRecordUiSideEffect.ShowSnackbar("이미 삭제됐거나 접근할 수 없는 기록이에요."))
                }
            }
        }

        private fun handleSaveFailure(error: Throwable) {
            updateState { copy(isSavingRecord = false) }
            if (error is HandledException) return
            val message =
                if (error is ApiException.NetworkException) {
                    "네트워크 상태를 확인한 뒤 다시 저장해주세요."
                } else {
                    "저장하지 못했어요. 잠시 후 다시 시도해주세요."
                }
            sendEffect(TimelineRecordUiSideEffect.ShowSnackbar(message))
        }

        private suspend fun discardDraftTracking(recordDate: LocalDate) {
            val activeTask =
                (draftTaskCoordinator.state.value as? DraftTaskTrackingState.WithTask)?.task
            if (activeTask?.recordDate == recordDate) {
                draftTaskCoordinator.discard()
            }
        }

        private fun requestDelete() {
            val record = state.value.editableRecord() ?: return
            if (state.value.deleteDialogState != TimelineDeleteDialogState.Hidden ||
                state.value.isSavingRecord ||
                state.value.memoEditor != null
            ) {
                return
            }
            updateState {
                copy(
                    deleteTarget =
                        TimelineRecordDeleteTarget(
                            recordDate = record.recordDate,
                        ),
                    deleteDialogState = TimelineDeleteDialogState.Confirmation,
                )
            }
        }

        private suspend fun deleteRecord() {
            val current = state.value
            val target = current.deleteTarget ?: return
            if (current.deleteDialogState == TimelineDeleteDialogState.Deleting ||
                current.deleteDialogState == TimelineDeleteDialogState.Success
            ) {
                return
            }

            updateState { copy(deleteDialogState = TimelineDeleteDialogState.Deleting) }
            deleteDailyRecordUseCase(target.recordDate)
                .onSuccess {
                    val activeTask =
                        (draftTaskCoordinator.state.value as? DraftTaskTrackingState.WithTask)?.task
                    if (activeTask?.recordDate == target.recordDate) {
                        draftTaskCoordinator.discard()
                    }
                    updateState { copy(deleteDialogState = TimelineDeleteDialogState.Success) }
                }.onFailure(::handleDeleteFailure)
        }

        private fun dismissDelete() {
            if (state.value.isDeleting) return
            clearDeleteDialog()
        }

        private fun clearDeleteDialog() {
            updateState {
                copy(
                    deleteTarget = null,
                    deleteDialogState = TimelineDeleteDialogState.Hidden,
                )
            }
        }

        private fun finishDelete() {
            if (state.value.deleteDialogState != TimelineDeleteDialogState.Success) return
            updateState {
                copy(
                    deleteTarget = null,
                    deleteDialogState = TimelineDeleteDialogState.Hidden,
                )
            }
            navigationHelper.navigateToBack()
        }

        private fun handleDeleteFailure(error: Throwable) {
            when (val action = error.toTimelineDeleteFailureAction()) {
                TimelineDeleteFailureAction.TargetUnavailable -> {
                    updateState {
                        copy(
                            content = TimelineRecordUiContent.Unavailable,
                            deleteTarget = null,
                            deleteDialogState = TimelineDeleteDialogState.Hidden,
                        )
                    }
                    sendEffect(TimelineRecordUiSideEffect.ShowSnackbar("이미 삭제됐거나 접근할 수 없는 기록이에요."))
                }
                TimelineDeleteFailureAction.RecordAlreadySaved -> {
                    clearDeleteDialog()
                    sendEffect(TimelineRecordUiSideEffect.ShowSnackbar("작성 완료된 기록은 삭제할 수 없어요."))
                }
                TimelineDeleteFailureAction.AlreadyHandled -> clearDeleteDialog()
                is TimelineDeleteFailureAction.Retryable ->
                    showRetryableDeleteError(action.message)
            }
        }

        private fun showRetryableDeleteError(message: String) {
            updateState {
                copy(deleteDialogState = TimelineDeleteDialogState.RetryableError(message))
            }
        }

        private fun editMemo(timelineEventId: Long) {
            val current = state.value
            if (current.memoEditor != null ||
                current.deleteDialogState != TimelineDeleteDialogState.Hidden ||
                current.isSavingRecord
            ) {
                return
            }
            val event =
                current
                    .editableRecord()
                    ?.events
                    ?.firstOrNull { it.timelineEventId == timelineEventId }
                    ?: return
            updateState {
                copy(
                    memoEditor =
                        TimelineMemoEditorState(
                            timelineEventId = timelineEventId,
                            originalMemo = event.memo.orEmpty(),
                            draftMemo = event.memo.orEmpty(),
                        ),
                )
            }
        }

        private fun changeMemo(value: String) {
            updateState {
                val editor = memoEditor ?: return@updateState this
                if (editor.isSaving) this else copy(memoEditor = editor.copy(draftMemo = value))
            }
        }

        private fun cancelMemoEdit() {
            if (state.value.memoEditor?.isSaving == true) return
            updateState { copy(memoEditor = null) }
        }

        private suspend fun updateMemo() {
            val editor = state.value.memoEditor ?: return
            if (!editor.isConfirmEnabled) return
            updateState { copy(memoEditor = editor.copy(isSaving = true)) }
            updateTimelineEventMemoUseCase(
                timelineEventId = editor.timelineEventId,
                memo = editor.draftMemo.takeUnless(String::isBlank),
            ).onSuccess {
                updateState { copy(memoEditor = null) }
            }.onFailure(::handleMemoUpdateFailure)
        }

        private fun handleMemoUpdateFailure(error: Throwable) {
            updateState {
                copy(memoEditor = memoEditor?.copy(isSaving = false))
            }
            when ((error as? TimelineEventUpdateException)?.reason) {
                TimelineEventUpdateException.Reason.INVALID_REQUEST ->
                    sendEffect(TimelineRecordUiSideEffect.ShowSnackbar("메모 내용을 다시 확인해 주세요."))
                TimelineEventUpdateException.Reason.EVENT_UNAVAILABLE -> {
                    updateState { copy(memoEditor = null) }
                    sendEffect(TimelineRecordUiSideEffect.ShowSnackbar("이미 삭제됐거나 접근할 수 없는 이벤트예요."))
                    requestedRecordDate?.let(::loadRecord)
                }
                TimelineEventUpdateException.Reason.RECORD_ALREADY_SAVED -> {
                    updateState { copy(memoEditor = null) }
                    sendEffect(TimelineRecordUiSideEffect.ShowSnackbar("작성 완료된 기록은 수정할 수 없어요."))
                }
                else -> handleFailure(error)
            }
        }

        private fun TimelineRecordUiState.editableRecord() = (content as? TimelineRecordUiContent.Record)?.value?.takeIf { it.isEditable }
    }
