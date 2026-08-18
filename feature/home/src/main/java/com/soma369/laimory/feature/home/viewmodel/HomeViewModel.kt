package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.DraftPhotoLimitExceededException
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.model.timeline.DraftTaskUnavailableReason
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.navigation.CollectionPage
import com.soma369.laimory.core.domain.navigation.DraftConsentPage
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.usecase.GetDailyRecordsUseCase
import com.soma369.laimory.core.domain.usecase.GetPhotosInWindowUseCase
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.domain.usecase.PrepareSelectedPhotosUseCase
import com.soma369.laimory.core.domain.usecase.PrepareTimelineDraftSelectionUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.home.draft.DraftConsentSessionStore
import com.soma369.laimory.feature.home.model.toPastRecordUiModel
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.DraftEndDay
import com.soma369.laimory.feature.home.state.DraftRetryMode
import com.soma369.laimory.feature.home.state.HomePastRecordsUiState
import com.soma369.laimory.feature.home.state.HomePhotoItem
import com.soma369.laimory.feature.home.state.HomeTimeField
import com.soma369.laimory.feature.home.state.HomeTimeSheetState
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiSideEffect
import com.soma369.laimory.feature.home.state.HomeUiState
import com.soma369.laimory.feature.home.state.MAX_PHOTO_SELECTION
import com.soma369.laimory.feature.home.state.isDateLocked
import com.soma369.laimory.feature.home.state.isInputLocked
import com.soma369.laimory.feature.home.state.nonPhotoSourceItems
import com.soma369.laimory.feature.home.state.refreshSourceSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
        private val prepareTimelineDraftSelectionUseCase: PrepareTimelineDraftSelectionUseCase,
        private val getDailyRecordsUseCase: GetDailyRecordsUseCase,
        private val getPhotosInWindowUseCase: GetPhotosInWindowUseCase,
        private val prepareSelectedPhotosUseCase: PrepareSelectedPhotosUseCase,
        private val draftConsentSessionStore: DraftConsentSessionStore,
        private val draftTaskCoordinator: DraftTaskCoordinator,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<HomeUiState, HomeUiIntent, HomeUiSideEffect>(
            HomeUiState(selectedDate = LocalDate.now(ZoneId.systemDefault())),
        ) {
        private val zone: ZoneId = ZoneId.systemDefault()
        private var sourceItems: List<SourceItem> = emptyList()
        private var photoCandidates: List<PhotoCandidate> = emptyList()
        private var photoAccessGranted = false
        private var photoCandidatesJob: Job? = null
        private var lastLoadedPhotoWindow: RecordDateWindow? = null
        private var requestedPhotoWindow: RecordDateWindow? = null
        private var preparedPhotoCache: PreparedPhotoCache? = null
        private var hasUserSelectedDate = false
        private var pastRecordsJob: Job? = null
        private var consentPreparationJob: Job? = null

        init {
            observeSummary()
            observeDraftTask()
        }

        override suspend fun handleIntent(intent: HomeUiIntent) {
            when (intent) {
                HomeUiIntent.NavigateToCollection -> navigationHelper.navigateTo(CollectionPage)
                HomeUiIntent.OpenDraftSheet -> openDraftSheet()
                HomeUiIntent.DismissDraftSheet -> updateState { copy(isDraftSheetVisible = false) }
                HomeUiIntent.OpenPhotoSheet -> requestPhotoSheet()
                HomeUiIntent.RequestAdditionalPhotoAccess ->
                    sendEffect(HomeUiSideEffect.RequestPhotoAccess(force = true))
                is HomeUiIntent.ResolvePhotoAccess -> resolvePhotoAccess(intent.granted, intent.limited)
                is HomeUiIntent.RefreshPhotos -> refreshPhotos(intent.hasAccess, intent.limited)
                HomeUiIntent.DismissPhotoSheet ->
                    updateState { copy(isPhotoSheetVisible = false, pendingPhotoIds = emptySet()) }
                is HomeUiIntent.TogglePhoto -> togglePhoto(intent.mediaStoreId)
                is HomeUiIntent.TogglePhotoDate -> togglePhotoDate(intent.date)
                HomeUiIntent.ToggleAllPhotos -> toggleAllPhotos()
                HomeUiIntent.ConfirmPhotoSelection -> confirmPhotoSelection()
                HomeUiIntent.ShowDatePicker -> updateState { copy(isDatePickerVisible = true) }
                HomeUiIntent.DismissDatePicker -> updateState { copy(isDatePickerVisible = false) }
                is HomeUiIntent.SelectDate -> selectDate(intent.date)
                is HomeUiIntent.ShowTimePicker -> showTimeSheet(intent.field)
                is HomeUiIntent.ExpandTimeField ->
                    updateState { copy(timeSheet = timeSheet?.copy(expandedField = intent.field)) }
                is HomeUiIntent.ChangeSheetTime -> changeSheetTime(intent.field, intent.date, intent.time)
                HomeUiIntent.ConfirmTimeSheet -> confirmTimeSheet()
                HomeUiIntent.DismissTimePicker -> updateState { copy(timeSheet = null) }
                HomeUiIntent.CreateDraft -> prepareDraftConsent()
                HomeUiIntent.ConsumeDraftConsentResult -> consumeDraftConsentResult()
                HomeUiIntent.RetryDraft -> retryDraft()
                HomeUiIntent.ContinueWaiting -> draftTaskCoordinator.continueWaiting()
                HomeUiIntent.StartNewDraft -> startNewDraft()
                HomeUiIntent.ViewDraft -> viewDraft()
                HomeUiIntent.SyncPastRecords -> syncPastRecords()
                is HomeUiIntent.SelectPastRecord ->
                    navigationHelper.navigateTo(TimelinePage(intent.recordDate))
            }
        }

        private fun observeSummary() =
            safeLaunch {
                observeSourceItemsUseCase().collect { items ->
                    sourceItems = items
                    updateState { refreshSourceSummary(items, photoCandidates, zone) }
                }
            }

        private fun observeDraftTask() =
            safeLaunch {
                draftTaskCoordinator.state.collect { trackingState ->
                    val previousWindow = state.value.recordDateWindow(zone)
                    updateState {
                        if (hasUserSelectedDate) {
                            withDraftTrackingForSelectedDate(trackingState)
                        } else {
                            withDraftTracking(trackingState)
                        }
                    }
                    if (state.value.recordDateWindow(zone) != previousWindow) {
                        onRecordWindowChanged()
                    }
                }
            }

        private fun openDraftSheet() {
            updateState { copy(isDraftSheetVisible = true) }
        }

        private fun requestPhotoSheet() {
            if (state.value.draftStatus.isInputLocked) return
            sendEffect(HomeUiSideEffect.RequestPhotoAccess())
        }

        private fun resolvePhotoAccess(
            granted: Boolean,
            limited: Boolean,
        ) {
            photoAccessGranted = granted
            if (!granted) {
                sendEffect(HomeUiSideEffect.ShowSnackbar("사진을 선택하려면 사진 접근 권한이 필요해요."))
                return
            }
            updateState {
                copy(
                    isPhotoSheetVisible = true,
                    pendingPhotoIds = selectedPhotoIds,
                    isPhotoLoading = true,
                    isPhotoAccessLimited = limited,
                )
            }
            loadPhotoCandidates(force = true)
        }

        private fun refreshPhotos(
            hasAccess: Boolean,
            limited: Boolean,
        ) {
            photoAccessGranted = hasAccess
            if (!hasAccess) {
                clearPhotoCandidates()
                return
            }
            updateState { copy(isPhotoAccessLimited = limited) }
            loadPhotoCandidates(force = true)
        }

        private fun togglePhoto(mediaStoreId: Long) {
            val current = state.value
            if (current.availablePhotos.none { it.mediaStoreId == mediaStoreId }) return
            if (mediaStoreId !in current.pendingPhotoIds && current.pendingPhotoIds.size >= MAX_PHOTO_SELECTION) {
                showPhotoLimitMessage()
                return
            }
            updateState {
                copy(
                    pendingPhotoIds =
                        if (mediaStoreId in pendingPhotoIds) {
                            pendingPhotoIds - mediaStoreId
                        } else {
                            pendingPhotoIds + mediaStoreId
                        },
                )
            }
        }

        private fun toggleAllPhotos() {
            val current = state.value
            val selectableIds =
                current.availablePhotos
                    .take(MAX_PHOTO_SELECTION)
                    .mapTo(linkedSetOf(), HomePhotoItem::mediaStoreId)
            val shouldClear =
                current.pendingPhotoIds.size == selectableIds.size &&
                    current.pendingPhotoIds.containsAll(selectableIds)
            updateState {
                copy(pendingPhotoIds = if (shouldClear) emptySet() else selectableIds)
            }
            if (!shouldClear && current.availablePhotos.size > MAX_PHOTO_SELECTION) showPhotoLimitMessage()
        }

        private fun togglePhotoDate(date: LocalDate) {
            val current = state.value
            val datePhotoIds =
                current.availablePhotos
                    .filter { it.capturedAt.atZone(zone).toLocalDate() == date }
                    .map(HomePhotoItem::mediaStoreId)
            if (datePhotoIds.isEmpty()) return
            val isDateSelected = current.pendingPhotoIds.containsAll(datePhotoIds)
            if (isDateSelected) {
                updateState { copy(pendingPhotoIds = pendingPhotoIds - datePhotoIds.toSet()) }
                return
            }

            val availableSlots = MAX_PHOTO_SELECTION - current.pendingPhotoIds.size
            val idsToAdd = datePhotoIds.filterNot(current.pendingPhotoIds::contains).take(availableSlots)
            updateState { copy(pendingPhotoIds = pendingPhotoIds + idsToAdd) }
            if (idsToAdd.size < datePhotoIds.count { it !in current.pendingPhotoIds }) showPhotoLimitMessage()
        }

        private fun confirmPhotoSelection() {
            if (state.value.draftStatus.isInputLocked) return
            preparedPhotoCache = null
            updateState {
                copy(
                    selectedPhotoIds = pendingPhotoIds,
                    pendingPhotoIds = emptySet(),
                    isPhotoSheetVisible = false,
                    draftStatus = DraftCreationStatus.IDLE,
                    draftRetryMode = null,
                    draftMessage = null,
                ).refreshSourceSummary(sourceItems, photoCandidates, zone)
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
                    .refreshSourceSummary(sourceItems, photoCandidates, zone)
                    .withDraftTrackingForSelectedDate(draftTaskCoordinator.state.value)
            }
            onRecordWindowChanged()
        }

        private fun showTimeSheet(field: HomeTimeField) {
            if (state.value.draftStatus.isInputLocked) return
            updateState {
                copy(
                    timeSheet =
                        HomeTimeSheetState(
                            startTime = startTime,
                            endDay = endDay,
                            endTime = endTime,
                            expandedField = field,
                        ),
                )
            }
        }

        /** 종료 줄의 날짜 롤러가 당일·익일 선택을 겸하므로, 고른 날짜를 기록 날짜와의 차이로 되돌린다. */
        private fun changeSheetTime(
            field: HomeTimeField,
            date: LocalDate,
            time: LocalTime,
        ) {
            updateState {
                val sheet = timeSheet ?: return@updateState this
                val next =
                    when (field) {
                        HomeTimeField.START -> sheet.copy(startTime = time)
                        HomeTimeField.END ->
                            sheet.copy(
                                endDay = if (date == selectedDate) DraftEndDay.SAME_DAY else DraftEndDay.NEXT_DAY,
                                endTime = time,
                            )
                    }
                copy(timeSheet = next)
            }
        }

        private fun confirmTimeSheet() {
            val sheet = state.value.timeSheet ?: return
            if (state.value.draftStatus.isInputLocked || !sheet.isConfirmEnabled) return
            updateState {
                val next =
                    copy(
                        startTime = sheet.startTime,
                        endDay = sheet.endDay,
                        endTime = sheet.endTime,
                        timeSheet = null,
                        draftStatus = DraftCreationStatus.IDLE,
                        draftRetryMode = null,
                        draftMessage = null,
                    )
                next.refreshSourceSummary(sourceItems, photoCandidates, zone)
            }
            onRecordWindowChanged()
        }

        /**
         * 전송 스냅샷을 확정하고 동의 화면으로 이동한다.
         *
         * 사진 업로드·초안 생성 API 는 여기서 호출하지 않는다 — 동의 화면에서 필수 동의를 모두
         * 완료하고 CTA 를 선택한 경우에만 시작된다. 사진 상한 초과·접근 불가 사진 같은 입력
         * 오류는 동의 화면으로 이동하지 않고 홈에서 바로 수정하도록 안내한다.
         */
        private fun prepareDraftConsent() {
            if (state.value.draftStatus.isInputLocked) return
            if (consentPreparationJob?.isActive == true) return
            // 동의 화면이 소비하지 않은 시도가 남아 있으면 중복 진입하지 않는다.
            if (draftConsentSessionStore.preparation.value != null) return
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
            consentPreparationJob =
                safeLaunch(
                    onError = ::handleDraftCreationFailure,
                ) {
                    val selectedPhotoItems = prepareSelectedPhotos(current) ?: return@safeLaunch
                    val selection =
                        prepareTimelineDraftSelectionUseCase(
                            window,
                            current.nonPhotoSourceItems(sourceItems, zone) + selectedPhotoItems,
                        ).getOrElse {
                            handleDraftCreationFailure(it)
                            return@safeLaunch
                        }
                    draftConsentSessionStore.prepare(
                        recordDate = current.selectedDate,
                        zone = zone,
                        window = window,
                        selection = selection,
                        discardActiveTask = shouldDiscardPreviousTask,
                    )
                    navigationHelper.navigateTo(DraftConsentPage)
                }
        }

        /**
         * 동의 화면 복귀 결과를 1회 확인한다 — 제출 완료면 시트를 닫고 시작을 알리고,
         * 제출 시점 사진 접근 실패면 사진 재선택 흐름을 연다.
         */
        private fun consumeDraftConsentResult() {
            if (draftConsentSessionStore.consumePhotoReselectionNeeded()) {
                preparedPhotoCache = null
                val message = "선택한 사진에 접근할 수 없어요. 사진을 다시 선택해주세요."
                updateState {
                    copy(
                        draftStatus = DraftCreationStatus.FAILED,
                        draftRetryMode = DraftRetryMode.NEW_DRAFT,
                        draftMessage = message,
                        isDraftSheetVisible = false,
                    )
                }
                sendEffect(HomeUiSideEffect.ShowSnackbar(message))
                sendEffect(HomeUiSideEffect.RequestPhotoAccess())
                return
            }
            if (!draftConsentSessionStore.consumeSubmittedResult()) return
            updateState { copy(isDraftSheetVisible = false) }
            sendEffect(HomeUiSideEffect.ShowSnackbar("초안 생성을 시작했어요."))
        }

        private suspend fun prepareSelectedPhotos(current: HomeUiState): List<SourceItem>? {
            val selectedIds =
                current.availablePhotos
                    .asSequence()
                    .filter { it.mediaStoreId in current.selectedPhotoIds }
                    .map(HomePhotoItem::mediaStoreId)
                    .toList()
            if (selectedIds.isEmpty()) return emptyList()

            preparedPhotoCache
                ?.takeIf { it.ids == selectedIds.toSet() }
                ?.let { return it.items }

            val prepared = prepareSelectedPhotosUseCase(selectedIds)
            if (prepared.unavailableIds.isNotEmpty()) {
                handleUnavailablePhotos(prepared.unavailableIds)
                return null
            }
            preparedPhotoCache =
                PreparedPhotoCache(
                    ids = selectedIds.toSet(),
                    items = prepared.items,
                )
            return prepared.items
        }

        private fun handleUnavailablePhotos(unavailableIds: Set<Long>) {
            photoCandidates = photoCandidates.filterNot { it.id in unavailableIds }
            preparedPhotoCache = null
            val message = "선택한 사진 ${unavailableIds.size}장에 접근할 수 없어요. 사진을 다시 선택해주세요."
            updateState {
                val remainingSelectedIds = selectedPhotoIds - unavailableIds
                copy(
                    selectedPhotoIds = remainingSelectedIds,
                    pendingPhotoIds = remainingSelectedIds,
                    draftStatus = DraftCreationStatus.FAILED,
                    draftRetryMode = DraftRetryMode.NEW_DRAFT,
                    draftMessage = message,
                    isDraftSheetVisible = false,
                    isPhotoSheetVisible = true,
                ).refreshSourceSummary(sourceItems, photoCandidates, zone)
            }
            sendEffect(HomeUiSideEffect.ShowSnackbar(message))
        }

        private fun handleDraftCreationFailure(error: Throwable) {
            if (error is DraftPhotoLimitExceededException) {
                val message = "${error.message}\n사진 선택에서 개수를 줄여주세요."
                updateState {
                    copy(
                        draftStatus = DraftCreationStatus.FAILED,
                        draftRetryMode = DraftRetryMode.NEW_DRAFT,
                        draftMessage = message,
                        isDraftSheetVisible = false,
                        isPhotoSheetVisible = true,
                        pendingPhotoIds = selectedPhotoIds,
                    )
                }
                sendEffect(HomeUiSideEffect.ShowSnackbar(message))
                return
            }

            updateState {
                copy(
                    draftStatus = DraftCreationStatus.FAILED,
                    draftRetryMode = DraftRetryMode.NEW_DRAFT,
                    draftMessage = "초안 생성 요청을 보내지 못했어요.",
                )
            }
            handleFailure(error)
        }

        private fun loadPhotoCandidates(force: Boolean) {
            if (!photoAccessGranted) return
            val window = state.value.recordDateWindow(zone) ?: return
            if (!force && window == requestedPhotoWindow) return
            if (!force && window == lastLoadedPhotoWindow) {
                photoCandidatesJob?.cancel()
                photoCandidatesJob = null
                requestedPhotoWindow = null
                updateState {
                    refreshSourceSummary(sourceItems, photoCandidates, zone)
                        .copy(isPhotoLoading = false)
                }
                return
            }

            photoCandidatesJob?.cancel()
            requestedPhotoWindow = window
            photoCandidatesJob =
                safeLaunch(
                    onError = { error ->
                        if (error !is CancellationException && requestedPhotoWindow == window) {
                            requestedPhotoWindow = null
                            updateState { copy(isPhotoLoading = false) }
                            handleFailure(error)
                        }
                    },
                ) {
                    updateState { copy(isPhotoLoading = true) }
                    val candidates = getPhotosInWindowUseCase(window)
                    photoCandidates = candidates
                    lastLoadedPhotoWindow = window
                    requestedPhotoWindow = null
                    preparedPhotoCache =
                        preparedPhotoCache?.takeIf { cache ->
                            val availableIds = candidates.mapTo(mutableSetOf(), PhotoCandidate::id)
                            cache.ids.all(availableIds::contains)
                        }
                    updateState {
                        refreshSourceSummary(sourceItems, candidates, zone)
                            .copy(isPhotoLoading = false)
                    }
                }
        }

        private fun onRecordWindowChanged() {
            preparedPhotoCache = null
            updateState { refreshSourceSummary(sourceItems, photoCandidates, zone) }
            loadPhotoCandidates(force = false)
        }

        private fun clearPhotoCandidates() {
            photoCandidatesJob?.cancel()
            photoCandidatesJob = null
            photoCandidates = emptyList()
            lastLoadedPhotoWindow = null
            requestedPhotoWindow = null
            preparedPhotoCache = null
            updateState {
                copy(
                    isPhotoLoading = false,
                    isPhotoSheetVisible = false,
                    isPhotoAccessLimited = false,
                ).refreshSourceSummary(sourceItems, emptyList(), zone)
            }
        }

        private fun showPhotoLimitMessage() {
            sendEffect(HomeUiSideEffect.ShowSnackbar("사진은 최대 ${MAX_PHOTO_SELECTION}장까지 선택할 수 있어요."))
        }

        private fun retryDraft() {
            when (state.value.draftRetryMode) {
                DraftRetryMode.POLLING -> draftTaskCoordinator.retry()
                DraftRetryMode.NEW_DRAFT -> prepareDraftConsent()
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
                navigationHelper.navigateTo(TimelinePage(trackingState.task.recordDate))
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
            val trackingTask = (trackingState as? DraftTaskTrackingState.WithTask)?.task
            val alignedState =
                if (trackingTask != null && trackingTask.recordDate != selectedDate) {
                    copy(selectedDate = trackingTask.recordDate)
                        .refreshSourceSummary(sourceItems, photoCandidates, zone)
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

        private data class PreparedPhotoCache(
            val ids: Set<Long>,
            val items: List<SourceItem>,
        )
    }
