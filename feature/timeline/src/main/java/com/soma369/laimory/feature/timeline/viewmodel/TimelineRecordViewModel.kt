package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.navigation.TimelineEventEditorPage
import com.soma369.laimory.core.domain.usecase.DeleteDailyRecordUseCase
import com.soma369.laimory.core.domain.usecase.ObserveTimelineRecordUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.timeline.model.toUiModel
import com.soma369.laimory.feature.timeline.state.TimelineDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineRecordDeleteTarget
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiContent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiSideEffect
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TimelineRecordViewModel
    @Inject
    constructor(
        observeTimelineRecordUseCase: ObserveTimelineRecordUseCase,
        private val deleteDailyRecordUseCase: DeleteDailyRecordUseCase,
        private val draftTaskCoordinator: DraftTaskCoordinator,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<TimelineRecordUiState, TimelineRecordUiIntent, TimelineRecordUiSideEffect>(
            TimelineRecordUiState(),
        ) {
        init {
            safeLaunch {
                observeTimelineRecordUseCase().collect { timeline ->
                    updateState {
                        copy(
                            content =
                                timeline
                                    ?.toUiModel()
                                    ?.let(TimelineRecordUiContent::Record)
                                    ?: TimelineRecordUiContent.Unavailable,
                        )
                    }
                }
            }
        }

        override suspend fun handleIntent(intent: TimelineRecordUiIntent) {
            when (intent) {
                TimelineRecordUiIntent.NavigateBack ->
                    if (!state.value.isDeleting) navigationHelper.navigateToBack()
                TimelineRecordUiIntent.RequestDelete -> requestDelete()
                TimelineRecordUiIntent.ConfirmDelete -> deleteRecord()
                TimelineRecordUiIntent.DismissDelete -> dismissDelete()
                TimelineRecordUiIntent.FinishDelete -> finishDelete()
                is TimelineRecordUiIntent.SelectEvent ->
                    if (state.value.deleteDialogState == TimelineDeleteDialogState.Hidden) {
                        navigationHelper.navigateTo(TimelineEventEditorPage(intent.timelineEventId))
                    }
            }
        }

        private fun requestDelete() {
            val record = (state.value.content as? TimelineRecordUiContent.Record)?.value ?: return
            if (state.value.deleteDialogState != TimelineDeleteDialogState.Hidden) return
            updateState {
                copy(
                    deleteTarget =
                        TimelineRecordDeleteTarget(
                            dailyRecordId = record.dailyRecordId,
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
            deleteDailyRecordUseCase(target.dailyRecordId)
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
    }
