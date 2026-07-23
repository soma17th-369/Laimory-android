package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.exception.TimelineRecordDeleteException
import com.soma369.laimory.core.domain.helper.NavigationHelper
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
                TimelineRecordUiIntent.OpenRecordMenu -> requestDelete()
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
                    draftTaskCoordinator.discard()
                    updateState { copy(deleteDialogState = TimelineDeleteDialogState.Success) }
                }.onFailure(::handleDeleteFailure)
        }

        private fun dismissDelete() {
            if (state.value.isDeleting) return
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
            when ((error as? TimelineRecordDeleteException)?.reason) {
                TimelineRecordDeleteException.Reason.TARGET_UNAVAILABLE -> {
                    updateState {
                        copy(
                            content = TimelineRecordUiContent.Unavailable,
                            deleteTarget = null,
                            deleteDialogState = TimelineDeleteDialogState.Hidden,
                        )
                    }
                    sendEffect(TimelineRecordUiSideEffect.ShowSnackbar("이미 삭제됐거나 접근할 수 없는 기록이에요."))
                }
                TimelineRecordDeleteException.Reason.RECORD_ALREADY_SAVED -> {
                    dismissDelete()
                    sendEffect(TimelineRecordUiSideEffect.ShowSnackbar("작성 완료된 기록은 삭제할 수 없어요."))
                }
                TimelineRecordDeleteException.Reason.DATE_OPERATION_IN_PROGRESS ->
                    showRetryableDeleteError("같은 날짜의 작업이 진행 중이에요. 잠시 후 다시 시도해주세요.")
                TimelineRecordDeleteException.Reason.PHOTO_DELETE_FAILED ->
                    showRetryableDeleteError("서버 사진을 삭제하지 못했어요. 잠시 후 다시 시도해주세요.")
                null -> {
                    val apiException =
                        when (error) {
                            is ApiException -> error
                            is HandledException -> error.cause as? ApiException
                            else -> null
                        }
                    if (apiException?.rawCode == 401) {
                        dismissDelete()
                    } else {
                        showRetryableDeleteError("네트워크 상태를 확인한 뒤 다시 시도해주세요.")
                    }
                }
            }
        }

        private fun showRetryableDeleteError(message: String) {
            updateState {
                copy(deleteDialogState = TimelineDeleteDialogState.RetryableError(message))
            }
        }
    }
