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
        private var saveJob: Job? = null

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
            // Record가 표시 중일 때만 재조회를 생략한다 — 저장 종결(Unavailable) 뒤 재진입이나
            // 실패 상태는 다시 조회해 재사용된 ViewModel의 잔여 상태를 서버 기준으로 되돌린다.
            val isAlreadyPresented =
                requestedRecordDate == recordDate &&
                    state.value.content is TimelineRecordUiContent.Record
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

        /**
         * 확인 없이 즉시 작성 완료를 요청한다.
         *
         * 네트워크 대기가 직렬 Intent 루프를 점유하면 저장 중 들어온 Intent가 가드 대신 큐에 쌓여
         * 실패 직후 재실행되므로, 요청은 별도 Job에서 수행하고 루프는 즉시 반환한다.
         */
        private fun saveRecord() {
            val current = state.value
            val record = current.editableRecord() ?: return
            if (current.isSavingRecord ||
                saveJob?.isActive == true ||
                current.deleteDialogState != TimelineDeleteDialogState.Hidden ||
                current.memoEditor != null
            ) {
                return
            }

            updateState { copy(isSavingRecord = true) }
            saveJob =
                safeLaunch(onError = ::handleSaveFailure) {
                    completeDailyRecordUseCase(record.recordDate)
                        .onSuccess { outcome -> handleSaveOutcome(outcome, record.recordDate) }
                        .onFailure(::handleSaveFailure)
                }
        }

        private suspend fun handleSaveOutcome(
            outcome: CompleteDailyRecordOutcome,
            recordDate: LocalDate,
        ) {
            // 확정·소실 모두 화면 상태를 먼저 종결한다 — 중복 요청을 차단하고, 엔트리 밖 수명으로
            // 재사용될 수 있는 ViewModel에 저장 중 상태가 남지 않게 한다.
            updateState { copy(content = TimelineRecordUiContent.Unavailable, isSavingRecord = false) }
            // 로컬 추적 정리 실패(DataStore I/O 등)는 이미 끝난 서버 저장을 되돌리지 않으므로
            // 결과 반영을 막지 않는다. 추적이 남아도 재진입 시 SAVED 기록을 읽기 전용으로 연다.
            runCatching { discardDraftTracking(recordDate) }
            when (outcome) {
                CompleteDailyRecordOutcome.Completed,
                CompleteDailyRecordOutcome.AlreadySaved,
                -> {
                    // 화면 side effect는 pop과 함께 수집이 끊길 수 있어 Root 수명 채널로 안내한다.
                    messageHelper.send(UserMessage.DailyRecordSaved)
                    navigationHelper.navigateToBack()
                }
                CompleteDailyRecordOutcome.RecordUnavailable ->
                    sendEffect(TimelineRecordUiSideEffect.ShowSnackbar("이미 삭제됐거나 접근할 수 없는 기록이에요."))
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
