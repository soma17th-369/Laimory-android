package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.model.timeline.DraftTaskUnavailableReason
import com.soma369.laimory.core.domain.navigation.CollectionPage
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.usecase.CreateTimelineDraftUseCase
import com.soma369.laimory.core.domain.usecase.GetDailyRecordsUseCase
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.home.model.toPastRecordUiModel
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.DraftEndDay
import com.soma369.laimory.feature.home.state.DraftRetryMode
import com.soma369.laimory.feature.home.state.HomePastRecordsUiState
import com.soma369.laimory.feature.home.state.HomeTimeField
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiSideEffect
import com.soma369.laimory.feature.home.state.HomeUiState
import com.soma369.laimory.feature.home.state.isDateLocked
import com.soma369.laimory.feature.home.state.isInputLocked
import com.soma369.laimory.feature.home.state.refreshSourceSummary
import com.soma369.laimory.feature.home.state.selectedSourceItems
import com.soma369.laimory.feature.home.state.withEndDaySelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val observeSourceItemsUseCase: ObserveSourceItemsUseCase,
        private val createTimelineDraftUseCase: CreateTimelineDraftUseCase,
        private val getDailyRecordsUseCase: GetDailyRecordsUseCase,
        private val draftTaskCoordinator: DraftTaskCoordinator,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<HomeUiState, HomeUiIntent, HomeUiSideEffect>(
            HomeUiState(selectedDate = LocalDate.now(ZoneId.systemDefault())),
        ) {
        private val zone: ZoneId = ZoneId.systemDefault()
        private var sourceItems: List<SourceItem> = emptyList()
        private var hasUserSelectedDate = false
        private var pastRecordsJob: Job? = null

        init {
            observeSummary()
            observeDraftTask()
        }

        override suspend fun handleIntent(intent: HomeUiIntent) {
            when (intent) {
                HomeUiIntent.NavigateToCollection -> navigationHelper.navigateTo(CollectionPage)
                HomeUiIntent.OpenDraftSheet -> openDraftSheet()
                HomeUiIntent.DismissDraftSheet -> updateState { copy(isDraftSheetVisible = false) }
                HomeUiIntent.OpenPhotoSheet -> openPhotoSheet()
                HomeUiIntent.DismissPhotoSheet ->
                    updateState { copy(isPhotoSheetVisible = false, pendingPhotoIds = emptySet()) }
                is HomeUiIntent.TogglePhoto -> togglePhoto(intent.rawId)
                is HomeUiIntent.TogglePhotoDate -> togglePhotoDate(intent.date)
                HomeUiIntent.ToggleAllPhotos -> toggleAllPhotos()
                HomeUiIntent.ConfirmPhotoSelection -> confirmPhotoSelection()
                HomeUiIntent.ShowDatePicker -> updateState { copy(isDatePickerVisible = true) }
                HomeUiIntent.DismissDatePicker -> updateState { copy(isDatePickerVisible = false) }
                is HomeUiIntent.SelectDate -> selectDate(intent.date)
                is HomeUiIntent.ShowTimePicker -> updateState { copy(editingTimeField = intent.field) }
                HomeUiIntent.DismissTimePicker -> updateState { copy(editingTimeField = null) }
                is HomeUiIntent.SelectTime -> selectTime(intent.field, intent.time)
                is HomeUiIntent.SelectEndDay -> selectEndDay(intent.endDay)
                HomeUiIntent.CreateDraft -> createDraft()
                HomeUiIntent.RetryDraft -> retryDraft()
                HomeUiIntent.ContinueWaiting -> draftTaskCoordinator.continueWaiting()
                HomeUiIntent.StartNewDraft -> startNewDraft()
                HomeUiIntent.ViewDraft -> viewDraft()
                HomeUiIntent.SyncPastRecords -> syncPastRecords()
                is HomeUiIntent.SelectPastRecord ->
                    navigationHelper.navigateTo(TimelinePage(intent.dailyRecordId))
            }
        }

        private fun observeSummary() =
            safeLaunch {
                observeSourceItemsUseCase().collect { items ->
                    sourceItems = items
                    updateState { refreshSourceSummary(items, zone) }
                }
            }

        private fun observeDraftTask() =
            safeLaunch {
                draftTaskCoordinator.state.collect { trackingState ->
                    updateState {
                        if (hasUserSelectedDate) {
                            withDraftTrackingForSelectedDate(trackingState)
                        } else {
                            withDraftTracking(trackingState)
                        }
                    }
                }
            }

        private fun openDraftSheet() {
            updateState { copy(isDraftSheetVisible = true) }
        }

        private fun openPhotoSheet() {
            if (state.value.draftStatus.isInputLocked) return
            updateState {
                copy(
                    isPhotoSheetVisible = true,
                    pendingPhotoIds = selectedPhotoIds,
                )
            }
        }

        private fun togglePhoto(rawId: String) {
            updateState {
                if (availablePhotos.none { it.rawId == rawId }) return@updateState this
                copy(
                    pendingPhotoIds =
                        if (rawId in pendingPhotoIds) {
                            pendingPhotoIds - rawId
                        } else {
                            pendingPhotoIds + rawId
                        },
                )
            }
        }

        private fun toggleAllPhotos() {
            updateState {
                val allPhotoIds = availablePhotos.mapTo(linkedSetOf()) { it.rawId }
                copy(
                    pendingPhotoIds =
                        if (pendingPhotoIds.size == allPhotoIds.size && pendingPhotoIds.containsAll(allPhotoIds)) {
                            emptySet()
                        } else {
                            allPhotoIds
                        },
                )
            }
        }

        private fun togglePhotoDate(date: LocalDate) {
            updateState {
                val datePhotoIds =
                    availablePhotos
                        .filter { it.capturedAt.atZone(zone).toLocalDate() == date }
                        .mapTo(linkedSetOf()) { it.rawId }
                if (datePhotoIds.isEmpty()) return@updateState this
                val isDateSelected = pendingPhotoIds.containsAll(datePhotoIds)
                copy(
                    pendingPhotoIds =
                        if (isDateSelected) {
                            pendingPhotoIds - datePhotoIds
                        } else {
                            pendingPhotoIds + datePhotoIds
                        },
                )
            }
        }

        private fun confirmPhotoSelection() {
            if (state.value.draftStatus.isInputLocked) return
            updateState {
                copy(
                    selectedPhotoIds = pendingPhotoIds,
                    pendingPhotoIds = emptySet(),
                    hasCustomizedPhotoSelection = true,
                    isPhotoSheetVisible = false,
                    draftStatus = DraftCreationStatus.IDLE,
                    draftRetryMode = null,
                    draftMessage = null,
                ).refreshSourceSummary(sourceItems, zone)
            }
        }

        private fun selectDate(date: LocalDate) {
            if (state.value.draftStatus.isDateLocked) return
            hasUserSelectedDate = true
            updateState {
                if (date == selectedDate) return@updateState copy(isDatePickerVisible = false)
                val next =
                    copy(
                        selectedDate = date,
                        startTime = LocalTime.MIDNIGHT,
                        endDay = DraftEndDay.NEXT_DAY,
                        endTime = LocalTime.MIDNIGHT,
                        isDatePickerVisible = false,
                        draftStatus = DraftCreationStatus.IDLE,
                        draftRetryMode = null,
                        draftMessage = null,
                    )
                next
                    .refreshSourceSummary(sourceItems, zone, resetPhotoSelection = true)
                    .withDraftTrackingForSelectedDate(draftTaskCoordinator.state.value)
            }
        }

        private fun selectTime(
            field: HomeTimeField,
            time: LocalTime,
        ) {
            if (state.value.draftStatus.isInputLocked) return
            updateState {
                val next =
                    when (field) {
                        HomeTimeField.START -> copy(startTime = time)
                        HomeTimeField.END -> copy(endTime = time)
                    }.copy(
                        editingTimeField = null,
                        draftStatus = DraftCreationStatus.IDLE,
                        draftRetryMode = null,
                        draftMessage = null,
                    )
                next.refreshSourceSummary(sourceItems, zone, resetPhotoSelection = true)
            }
        }

        private fun selectEndDay(endDay: DraftEndDay) {
            if (state.value.draftStatus.isInputLocked) return
            updateState {
                val next =
                    withEndDaySelection(endDay)
                        .copy(
                            draftStatus = DraftCreationStatus.IDLE,
                            draftRetryMode = null,
                            draftMessage = null,
                        )
                next.refreshSourceSummary(sourceItems, zone, resetPhotoSelection = true)
            }
        }

        private fun createDraft() {
            if (state.value.draftStatus.isInputLocked) {
                return
            }
            val current = state.value
            val window = current.recordDateWindow(zone)
            if (window == null) {
                sendEffect(HomeUiSideEffect.ShowSnackbar("종료 시각은 시작 시각보다 뒤로 설정해주세요."))
                return
            }
            if (current.summary.totalItemCount == 0) {
                sendEffect(HomeUiSideEffect.ShowSnackbar("선택한 범위에 모인 데이터가 없어요."))
                return
            }

            val shouldDiscardPreviousTask = current.draftRetryMode == DraftRetryMode.NEW_DRAFT
            updateState {
                copy(
                    draftStatus = DraftCreationStatus.SUBMITTING,
                    draftRetryMode = null,
                    draftMessage = null,
                )
            }
            safeLaunch(
                onError = {
                    updateState {
                        copy(
                            draftStatus = DraftCreationStatus.FAILED,
                            draftRetryMode = DraftRetryMode.NEW_DRAFT,
                            draftMessage = "초안 생성 요청을 보내지 못했어요.",
                        )
                    }
                    handleFailure(it)
                },
            ) {
                if (shouldDiscardPreviousTask) draftTaskCoordinator.discard()
                val result =
                    createTimelineDraftUseCase(
                        current.selectedDate,
                        zone,
                        window,
                        current.selectedSourceItems(sourceItems, zone),
                    )
                val handle =
                    result.getOrElse {
                        updateState {
                            copy(
                                draftStatus = DraftCreationStatus.FAILED,
                                draftRetryMode = DraftRetryMode.NEW_DRAFT,
                                draftMessage = "초안 생성 요청을 보내지 못했어요.",
                            )
                        }
                        handleFailure(it)
                        return@safeLaunch
                    }
                draftTaskCoordinator.start(handle.taskId, current.selectedDate)
                updateState { copy(isDraftSheetVisible = false) }
                sendEffect(HomeUiSideEffect.ShowSnackbar("초안 생성을 시작했어요."))
            }
        }

        private fun retryDraft() {
            when (state.value.draftRetryMode) {
                DraftRetryMode.POLLING -> draftTaskCoordinator.retry()
                DraftRetryMode.NEW_DRAFT -> createDraft()
                null -> Unit
            }
        }

        private fun startNewDraft() =
            safeLaunch {
                draftTaskCoordinator.discard()
            }

        private fun viewDraft() =
            safeLaunch {
                if (state.value.draftStatus != DraftCreationStatus.SUCCESS) return@safeLaunch
                val trackingState =
                    draftTaskCoordinator.state.value as? DraftTaskTrackingState.Success ?: return@safeLaunch
                navigationHelper.navigateTo(TimelinePage(trackingState.dailyRecordId))
            }

        /** 지난 기록 목록을 서버와 동기화한다. 진행 중이면 중복 요청하지 않는다. */
        private fun syncPastRecords() {
            if (pastRecordsJob?.isActive == true) return
            pastRecordsJob =
                safeLaunch(
                    onError = {
                        markPastRecordsFailure()
                        handleFailure(it)
                    },
                ) {
                    // 이미 목록을 보여주는 중이면 유지한 채 재동기화한다. (깜빡임 방지)
                    if (state.value.pastRecords !is HomePastRecordsUiState.Content) {
                        updateState { copy(pastRecords = HomePastRecordsUiState.Loading) }
                    }
                    getDailyRecordsUseCase()
                        .onSuccess { timelines ->
                            updateState {
                                copy(
                                    pastRecords =
                                        if (timelines.isEmpty()) {
                                            HomePastRecordsUiState.Empty
                                        } else {
                                            HomePastRecordsUiState.Content(
                                                timelines.map(DailyTimeline::toPastRecordUiModel),
                                            )
                                        },
                                )
                            }
                        }.onFailure { error ->
                            markPastRecordsFailure()
                            handleFailure(error)
                        }
                }
        }

        private fun markPastRecordsFailure() {
            updateState {
                if (pastRecords is HomePastRecordsUiState.Content) {
                    this
                } else {
                    copy(pastRecords = HomePastRecordsUiState.LoadFailed)
                }
            }
        }

        private fun HomeUiState.withDraftTracking(trackingState: DraftTaskTrackingState): HomeUiState {
            if (draftStatus == DraftCreationStatus.SUBMITTING && trackingState == DraftTaskTrackingState.Idle) {
                return this
            }
            val trackingTask = (trackingState as? DraftTaskTrackingState.WithTask)?.task
            val alignedState =
                if (trackingTask != null && trackingTask.recordDate != selectedDate) {
                    copy(selectedDate = trackingTask.recordDate)
                        .refreshSourceSummary(sourceItems, zone, resetPhotoSelection = true)
                } else {
                    this
                }
            return when (trackingState) {
                DraftTaskTrackingState.Idle -> alignedState.resetDraftPresentation()
                is DraftTaskTrackingState.Processing ->
                    alignedState.copy(
                        draftStatus = DraftCreationStatus.PROCESSING,
                        draftRetryMode = null,
                        draftMessage = processingMessage(trackingState.elapsedSeconds),
                    )

                is DraftTaskTrackingState.LongRunning ->
                    alignedState.copy(
                        draftStatus = DraftCreationStatus.LONG_RUNNING,
                        draftRetryMode = null,
                        draftMessage =
                            "초안 생성 시작 후 ${trackingState.elapsedSeconds / 60}분이 지났어요. " +
                                "계속 기다리거나 새로 만들 수 있어요.",
                    )

                is DraftTaskTrackingState.Success ->
                    alignedState.copy(
                        draftStatus = DraftCreationStatus.SUCCESS,
                        draftRetryMode = null,
                        draftMessage = "초안이 준비됐어요.",
                        isDraftSheetVisible = false,
                    )

                is DraftTaskTrackingState.Failed ->
                    alignedState.copy(
                        draftStatus = DraftCreationStatus.FAILED,
                        draftRetryMode = DraftRetryMode.NEW_DRAFT,
                        draftMessage = "초안을 만들지 못했어요. 다시 시도해주세요.",
                    )

                is DraftTaskTrackingState.RetryableError ->
                    alignedState.copy(
                        draftStatus = DraftCreationStatus.FAILED,
                        draftRetryMode = DraftRetryMode.POLLING,
                        draftMessage = "네트워크 상태를 확인한 뒤 상태를 다시 확인해주세요.",
                    )

                is DraftTaskTrackingState.Unavailable ->
                    alignedState.copy(
                        draftStatus = DraftCreationStatus.FAILED,
                        draftRetryMode = DraftRetryMode.NEW_DRAFT,
                        draftMessage =
                            when (trackingState.reason) {
                                DraftTaskUnavailableReason.TASK -> "초안 작업 정보를 찾을 수 없어요. 새로 만들어주세요."
                                DraftTaskUnavailableReason.RESULT -> "완료된 초안 결과를 찾을 수 없어요. 새로 만들어주세요."
                            },
                    )
            }
        }

        private fun HomeUiState.withDraftTrackingForSelectedDate(trackingState: DraftTaskTrackingState): HomeUiState {
            val trackingTask = (trackingState as? DraftTaskTrackingState.WithTask)?.task
            return if (trackingTask?.recordDate == selectedDate) {
                withDraftTracking(trackingState)
            } else {
                resetDraftPresentation()
            }
        }

        private fun HomeUiState.resetDraftPresentation(): HomeUiState =
            copy(
                draftStatus = DraftCreationStatus.IDLE,
                draftRetryMode = null,
                draftMessage = null,
            )

        private fun processingMessage(elapsedSeconds: Long?): String =
            when {
                elapsedSeconds == null -> "초안을 만들고 있어요."
                elapsedSeconds < 60L -> "초안을 만들고 있어요. ${elapsedSeconds}초 지났어요."
                else -> "초안을 만들고 있어요. ${elapsedSeconds / 60}분 지났어요."
            }
    }
