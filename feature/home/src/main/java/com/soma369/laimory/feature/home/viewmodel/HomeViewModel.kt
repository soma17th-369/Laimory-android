package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.navigation.CollectionPage
import com.soma369.laimory.core.domain.usecase.CreateTimelineDraftUseCase
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.DraftEndDay
import com.soma369.laimory.feature.home.state.HomeTimeField
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiSideEffect
import com.soma369.laimory.feature.home.state.HomeUiState
import com.soma369.laimory.feature.home.state.refreshSourceSummary
import com.soma369.laimory.feature.home.state.selectedSourceItems
import com.soma369.laimory.feature.home.state.withEndDaySelection
import dagger.hilt.android.lifecycle.HiltViewModel
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
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<HomeUiState, HomeUiIntent, HomeUiSideEffect>(
            HomeUiState(selectedDate = LocalDate.now(ZoneId.systemDefault())),
        ) {
        private val zone: ZoneId = ZoneId.systemDefault()
        private var sourceItems: List<SourceItem> = emptyList()

        init {
            observeSummary()
        }

        override suspend fun handleIntent(intent: HomeUiIntent) {
            when (intent) {
                HomeUiIntent.NavigateToCollection -> navigationHelper.navigateTo(CollectionPage)
                HomeUiIntent.OpenDraftSheet -> updateState { copy(isDraftSheetVisible = true) }
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
            }
        }

        private fun observeSummary() =
            safeLaunch {
                observeSourceItemsUseCase().collect { items ->
                    sourceItems = items
                    updateState { refreshSourceSummary(items, zone) }
                }
            }

        private fun openPhotoSheet() {
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
            if (state.value.draftStatus == DraftCreationStatus.SUBMITTING) return
            updateState {
                copy(
                    selectedPhotoIds = pendingPhotoIds,
                    pendingPhotoIds = emptySet(),
                    hasCustomizedPhotoSelection = true,
                    isPhotoSheetVisible = false,
                    draftStatus = DraftCreationStatus.IDLE,
                ).refreshSourceSummary(sourceItems, zone)
            }
        }

        private fun selectDate(date: LocalDate) {
            if (state.value.draftStatus == DraftCreationStatus.SUBMITTING) return
            updateState {
                val next =
                    copy(
                        selectedDate = date,
                        startTime = LocalTime.MIDNIGHT,
                        endDay = DraftEndDay.NEXT_DAY,
                        endTime = LocalTime.MIDNIGHT,
                        isDatePickerVisible = false,
                        draftStatus = DraftCreationStatus.IDLE,
                    )
                next.refreshSourceSummary(sourceItems, zone, resetPhotoSelection = true)
            }
        }

        private fun selectTime(
            field: HomeTimeField,
            time: LocalTime,
        ) {
            if (state.value.draftStatus == DraftCreationStatus.SUBMITTING) return
            updateState {
                val next =
                    when (field) {
                        HomeTimeField.START -> copy(startTime = time)
                        HomeTimeField.END -> copy(endTime = time)
                    }.copy(editingTimeField = null, draftStatus = DraftCreationStatus.IDLE)
                next.refreshSourceSummary(sourceItems, zone, resetPhotoSelection = true)
            }
        }

        private fun selectEndDay(endDay: DraftEndDay) {
            if (state.value.draftStatus == DraftCreationStatus.SUBMITTING) return
            updateState {
                val next =
                    withEndDaySelection(endDay)
                        .copy(draftStatus = DraftCreationStatus.IDLE)
                next.refreshSourceSummary(sourceItems, zone, resetPhotoSelection = true)
            }
        }

        private fun createDraft() {
            if (state.value.draftStatus in
                setOf(DraftCreationStatus.SUBMITTING, DraftCreationStatus.SUBMITTED)
            ) {
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

            updateState { copy(draftStatus = DraftCreationStatus.SUBMITTING) }
            safeLaunch(
                onError = {
                    updateState { copy(draftStatus = DraftCreationStatus.FAILED) }
                    handleFailure(it)
                },
            ) {
                createTimelineDraftUseCase(
                    current.selectedDate,
                    zone,
                    window,
                    current.selectedSourceItems(sourceItems, zone),
                )
                    .onSuccess {
                        updateState {
                            copy(
                                draftStatus = DraftCreationStatus.SUBMITTED,
                                isDraftSheetVisible = false,
                            )
                        }
                        sendEffect(HomeUiSideEffect.ShowSnackbar("초안 생성 요청을 보냈어요."))
                    }.onFailure {
                        updateState { copy(draftStatus = DraftCreationStatus.FAILED) }
                        handleFailure(it)
                    }
            }
        }
    }
