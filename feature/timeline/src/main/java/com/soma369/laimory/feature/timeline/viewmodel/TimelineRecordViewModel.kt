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
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.navigation.TimelineEventCreatePage
import com.soma369.laimory.core.domain.navigation.TimelineEventEditorPage
import com.soma369.laimory.core.domain.usecase.CompleteDailyRecordOutcome
import com.soma369.laimory.core.domain.usecase.CompleteDailyRecordUseCase
import com.soma369.laimory.core.domain.usecase.DeleteDailyRecordUseCase
import com.soma369.laimory.core.domain.usecase.DeleteTimelineEventUseCase
import com.soma369.laimory.core.domain.usecase.GetDailyRecordUseCase
import com.soma369.laimory.core.domain.usecase.ObserveTimelineRecordUseCase
import com.soma369.laimory.core.domain.usecase.SaveTimelineRecordUseCase
import com.soma369.laimory.core.domain.usecase.UpdateDailyRecordEmotionOutcome
import com.soma369.laimory.core.domain.usecase.UpdateDailyRecordEmotionUseCase
import com.soma369.laimory.core.domain.usecase.UpdateTimelineEventMemoUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.timeline.model.initialMode
import com.soma369.laimory.feature.timeline.model.timelineEmotionDateLabel
import com.soma369.laimory.feature.timeline.model.toUiModel
import com.soma369.laimory.feature.timeline.state.TimelineDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineEmotionSheetPurpose
import com.soma369.laimory.feature.timeline.state.TimelineEmotionSheetState
import com.soma369.laimory.feature.timeline.state.TimelineEventDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineMemoEditorState
import com.soma369.laimory.feature.timeline.state.TimelineRecordDeleteTarget
import com.soma369.laimory.feature.timeline.state.TimelineRecordMode
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiContent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiSideEffect
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class TimelineRecordViewModel
    @Inject
    constructor(
        observeTimelineRecordUseCase: ObserveTimelineRecordUseCase,
        private val getDailyRecordUseCase: GetDailyRecordUseCase,
        private val saveTimelineRecordUseCase: SaveTimelineRecordUseCase,
        private val completeDailyRecordUseCase: CompleteDailyRecordUseCase,
        private val updateDailyRecordEmotionUseCase: UpdateDailyRecordEmotionUseCase,
        private val updateTimelineEventMemoUseCase: UpdateTimelineEventMemoUseCase,
        private val deleteDailyRecordUseCase: DeleteDailyRecordUseCase,
        private val deleteTimelineEventUseCase: DeleteTimelineEventUseCase,
        private val draftTaskCoordinator: DraftTaskCoordinator,
        private val navigationHelper: NavigationHelper,
        private val messageHelper: MessageHelper,
        private val clock: Clock,
    ) : BaseMviViewModel<TimelineRecordUiState, TimelineRecordUiIntent, TimelineRecordUiSideEffect>(
            TimelineRecordUiState(),
        ) {
        private var requestedRecordDate: LocalDate? = null

        /**
         * 최초 진입 모드를 이미 적용한 기록 날짜.
         *
         * 세션은 메모 저장·Event 편집마다 다시 방출되는데, 그때마다 기록 상태로 모드를 되돌리면
         * 편집 중이던 화면이 읽기 모드로 튕긴다. 기록이 바뀔 때 한 번만 적용한다.
         */
        private var modeAppliedFor: LocalDate? = null
        private var loadJob: Job? = null
        private var saveJob: Job? = null
        private var eventDeleteJob: Job? = null

        /**
         * 이벤트별로 도는 메모 커밋.
         *
         * 같은 이벤트의 다음 요청은 앞엣것이 끝난 뒤에 나간다 — 엇갈려 도착하면 옛 값이 서버에
         * 남는다.
         */
        private val memoCommitJobs = mutableMapOf<Long, Job>()

        init {
            safeLaunch {
                observeTimelineRecordUseCase().collect { timeline ->
                    val requestedDate = requestedRecordDate ?: return@collect
                    updateState {
                        when {
                            timeline?.recordDate == requestedDate -> {
                                val record = timeline.toUiModel()
                                val isFirstApply = modeAppliedFor != requestedDate
                                modeAppliedFor = requestedDate
                                copy(
                                    content = TimelineRecordUiContent.Record(record),
                                    mode =
                                        if (isFirstApply) record.initialMode() else mode,
                                )
                            }

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
                TimelineRecordUiIntent.RequestSave -> openEmotionSheet()
                is TimelineRecordUiIntent.SelectEmotion -> selectEmotion(intent.emotion)
                TimelineRecordUiIntent.AddEvent -> addEvent()
                TimelineRecordUiIntent.EditEmotion -> openEmotionEditor()
                TimelineRecordUiIntent.ConfirmEmotion -> confirmEmotion()
                TimelineRecordUiIntent.DismissEmotionSheet -> dismissEmotionSheet()
                TimelineRecordUiIntent.EnterEditMode -> switchMode(TimelineRecordMode.EDIT)
                TimelineRecordUiIntent.ExitEditMode -> switchMode(TimelineRecordMode.READ)
                TimelineRecordUiIntent.RequestDelete -> requestDelete()
                TimelineRecordUiIntent.ConfirmDelete -> deleteRecord()
                TimelineRecordUiIntent.DismissDelete -> dismissDelete()
                TimelineRecordUiIntent.FinishDelete -> finishDelete()
                is TimelineRecordUiIntent.RequestEventDelete -> requestEventDelete(intent.timelineEventId)
                TimelineRecordUiIntent.ConfirmEventDelete -> deleteEvent()
                TimelineRecordUiIntent.DismissEventDelete -> dismissEventDelete()
                is TimelineRecordUiIntent.SelectEvent -> editEvent(intent.timelineEventId)
                is TimelineRecordUiIntent.EditMemo -> editMemo(intent.timelineEventId)
                is TimelineRecordUiIntent.ChangeMemo -> changeMemo(intent.value)
                is TimelineRecordUiIntent.CommitMemoEdit -> commitMemo(intent.timelineEventId)
                is TimelineRecordUiIntent.RetryMemoCommit -> sendMemo(intent.timelineEventId, intent.memo)
            }
        }

        private fun navigateBack() {
            val current = state.value
            if (current.isDeleting || current.isSavingRecord) return
            // 메모를 쓰던 중이면 갈무리만 하고 화면에 남는다 — 뒤로 한 번은 포커스를 놓는 동작이다.
            if (current.memoEditor != null) {
                commitOpenMemo()
                return
            }
            navigationHelper.navigateToBack()
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
            // 새로 조회하면 초기 모드도 다시 정한다.
            modeAppliedFor = null
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
                                    // 초기 모드는 세션 방출이 아니라 조회 결과로 정한다 —
                                    // 세션에 남아 있던 이전 값이 모드를 결정하면 안 된다.
                                    val record = outcome.value.toUiModel()
                                    modeAppliedFor = recordDate
                                    updateState {
                                        copy(
                                            content = TimelineRecordUiContent.Record(record),
                                            mode = record.initialMode(),
                                        )
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
         * 화면 모드를 바꾼다.
         *
         * 서버 요청도 `DRAFT ↔ SAVED` 전이도 일으키지 않는다. 진행 중인 작업이 있으면 바꾸지 않는다 —
         * 화면에서는 `X` 가 비활성이라 눌리지 않지만, Intent 경로에서도 한 번 더 막는다.
         */
        private fun switchMode(mode: TimelineRecordMode) {
            commitOpenMemo()
            val current = state.value
            val record = current.record() ?: return
            if (current.mode == mode || !current.isModeSwitchable) return
            // DRAFT 는 편집이 기본이라 읽기 모드로 나가지 않는다 — 화면에도 닫기 버튼이 없다.
            if (mode == TimelineRecordMode.READ && !record.isSaved) return
            updateState { copy(mode = mode) }
        }

        /** 저장 CTA는 감정 선택 시트를 여는 데까지만 관여한다 — 실제 저장은 시트의 `확인`이 일으킨다. */
        private fun openEmotionSheet() {
            commitOpenMemo()
            val current = state.value
            val record = current.unsavedRecord() ?: return
            if (current.emotionSheet != null ||
                current.isSavingRecord ||
                saveJob?.isActive == true ||
                current.deleteDialogState != TimelineDeleteDialogState.Hidden ||
                current.eventDeleteDialogState != TimelineEventDeleteDialogState.Hidden
            ) {
                return
            }
            val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
            updateState {
                copy(
                    emotionSheet =
                        TimelineEmotionSheetState(
                            recordDate = record.recordDate,
                            dateLabel = timelineEmotionDateLabel(record.recordDate, today),
                        ),
                )
            }
        }

        /** 빈 편집기를 열어 새 이벤트를 만든다. 편집 모드에서만, 진행 중인 작업이 없을 때만 연다. */
        private fun addEvent() {
            commitOpenMemo()
            val current = state.value
            val record = (current.content as? TimelineRecordUiContent.Record)?.value ?: return
            if (!current.mode.isEditing || !current.isModeSwitchable) return
            navigationHelper.navigateTo(TimelineEventCreatePage(record.recordDate))
        }

        /** 카드의 연필 — 이벤트 편집 화면으로 간다. */
        private fun editEvent(timelineEventId: Long) {
            commitOpenMemo()
            val current = state.value
            if (!current.mode.isEditing || !current.isModeSwitchable) return
            navigationHelper.navigateTo(TimelineEventEditorPage(timelineEventId))
        }

        /**
         * 저장된 기록의 감정을 바꾸기 위해 시트를 연다.
         *
         * 저장 전(DRAFT)에는 열리지 않는다 — 최초 감정은 작성 완료가 정하고, 서버도 DRAFT 에는
         * `409/-1020` 을 낸다. 읽기 모드에서도 열지 않는다: 감정은 보여 주기만 한다.
         */
        private fun openEmotionEditor() {
            commitOpenMemo()
            val current = state.value
            val record = (current.content as? TimelineRecordUiContent.Record)?.value ?: return
            if (!record.isSaved || !current.mode.isEditing) return
            if (current.emotionSheet != null || !current.isModeSwitchable) return
            val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
            updateState {
                copy(
                    emotionSheet =
                        TimelineEmotionSheetState(
                            recordDate = record.recordDate,
                            dateLabel = timelineEmotionDateLabel(record.recordDate, today),
                            // 지금 감정을 미리 골라 둔다. 바꾸러 온 사람에게 기본값을 다시 고르게 하면
                            // 무엇이 현재 값인지 시트가 알려 주지 않는 셈이다.
                            selected = record.emotion ?: TimelineEmotion.DEFAULT_SELECTION,
                            purpose = TimelineEmotionSheetPurpose.EDIT_EMOTION,
                        ),
                )
            }
        }

        /** 시트가 열린 목적에 따라 서버 경로가 갈린다. */
        private fun confirmEmotion() {
            when (state.value.emotionSheet?.purpose) {
                TimelineEmotionSheetPurpose.SAVE_RECORD -> saveRecord()
                TimelineEmotionSheetPurpose.EDIT_EMOTION -> updateEmotion()
                null -> Unit
            }
        }

        /**
         * 저장된 기록의 감정만 교체한다.
         *
         * 저장과 달리 화면을 떠나지 않는다 — 감정만 바뀌고 기록은 그 자리에 남는다. 성공하면
         * 다시 조회하지 않고 화면의 값만 갈아 끼운다. 서버가 멱등이라 같은 값을 눌러도 성공이다.
         */
        private fun updateEmotion() {
            val current = state.value
            val sheet = current.emotionSheet ?: return
            if (current.isSavingRecord) return
            updateState { copy(isSavingRecord = true) }
            saveJob =
                safeLaunch(onError = {
                    finishEmotionUpdate()
                    handleFailure(it)
                }) {
                    val outcome =
                        updateDailyRecordEmotionUseCase(sheet.recordDate, sheet.selected)
                            .getOrElse {
                                finishEmotionUpdate()
                                return@safeLaunch
                            }
                    when (outcome) {
                        UpdateDailyRecordEmotionOutcome.Updated -> applyEmotion(sheet.selected)
                        // 화면이 DRAFT 진입을 막으므로 여기 오면 화면과 서버가 어긋난 것이다.
                        UpdateDailyRecordEmotionOutcome.NotSaved ->
                            showSnackbar("먼저 하루 기록을 저장해 주세요.")

                        UpdateDailyRecordEmotionOutcome.RecordUnavailable ->
                            showSnackbar("이 기록을 찾을 수 없어요.")
                    }
                    finishEmotionUpdate()
                }
        }

        private fun showSnackbar(message: String) = sendEffect(TimelineRecordUiSideEffect.ShowSnackbar(message))

        private fun applyEmotion(emotion: TimelineEmotion) {
            updateState {
                val record = (content as? TimelineRecordUiContent.Record)?.value ?: return@updateState this
                copy(content = TimelineRecordUiContent.Record(record.copy(emotion = emotion)))
            }
        }

        private fun finishEmotionUpdate() {
            updateState { copy(isSavingRecord = false, emotionSheet = null) }
        }

        private fun selectEmotion(emotion: TimelineEmotion) {
            val current = state.value
            if (current.emotionSheet == null || current.isSavingRecord) return
            updateState { copy(emotionSheet = emotionSheet?.copy(selected = emotion)) }
        }

        private fun dismissEmotionSheet() {
            // 저장 요청이 떠 있는 동안 시트를 닫으면 완료 시점의 뒤늦은 pop·안내가 갈 곳을 잃는다.
            if (state.value.isSavingRecord) return
            updateState { copy(emotionSheet = null) }
        }

        /**
         * 시트에서 고른 감정으로 작성 완료를 요청한다.
         *
         * 네트워크 대기가 직렬 Intent 루프를 점유하면 저장 중 들어온 Intent가 가드 대신 큐에 쌓여
         * 실패 직후 재실행되므로, 요청은 별도 Job에서 수행하고 루프는 즉시 반환한다.
         */
        private fun saveRecord() {
            val current = state.value
            val record = current.unsavedRecord() ?: return
            val sheet = current.emotionSheet ?: return
            if (current.isSavingRecord ||
                saveJob?.isActive == true ||
                current.deleteDialogState != TimelineDeleteDialogState.Hidden ||
                current.eventDeleteDialogState != TimelineEventDeleteDialogState.Hidden
            ) {
                return
            }

            updateState { copy(isSavingRecord = true) }
            saveJob =
                safeLaunch(onError = ::handleSaveFailure) {
                    // 메모가 아직 날아가는 중일 수 있다. 기록이 먼저 확정되면 화면이 종결돼
                    // 뒤늦게 도착한 메모가 어디에 붙을지 알 수 없다.
                    awaitMemoCommits()
                    completeDailyRecordUseCase(record.recordDate, sheet.selected)
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
            updateState { copy(content = TimelineRecordUiContent.Unavailable, emotionSheet = null, isSavingRecord = false) }
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
            commitOpenMemo()
            // 하루 기록 삭제는 내용 편집과 별개인 기록 단위 관리 동작이라 SAVED 기록과 읽기 모드에서도 연다.
            val record = (state.value.content as? TimelineRecordUiContent.Record)?.value ?: return
            if (state.value.deleteDialogState != TimelineDeleteDialogState.Hidden ||
                state.value.isSavingRecord ||
                state.value.eventDeleteDialogState != TimelineEventDeleteDialogState.Hidden
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

        /**
         * 카드의 휴지통 — 지울 이벤트를 정하고 확인 창을 연다.
         *
         * 읽기 모드에서는 버튼이 없지만 Intent 경로도 막는다. 화면 밖에서 들어온 요청이 읽는
         * 화면의 내용을 지우면 안 된다.
         */
        private fun requestEventDelete(timelineEventId: Long) {
            commitOpenMemo()
            val current = state.value
            if (!current.mode.isEditing || !current.isModeSwitchable) return
            val exists =
                current
                    .record()
                    ?.events
                    ?.any { it.timelineEventId == timelineEventId } == true
            if (!exists) return
            updateState {
                copy(eventDeleteDialogState = TimelineEventDeleteDialogState.Confirmation(timelineEventId))
            }
        }

        /**
         * 확인·재시도 모두 이 경로다.
         *
         * 대상은 다이얼로그 상태가 들고 있는 값을 쓴다 — 목록은 그사이에도 갱신되므로 화면에서
         * 다시 찾으면 다른 이벤트를 지울 수 있다.
         *
         * 요청은 별도 Job 에서 돈다. 직렬 Intent 루프에서 응답을 기다리면 그동안 들어온 Intent 가
         * 가드를 거치지 못한 채 큐에 쌓였다가, 삭제가 끝나 상태가 풀린 뒤에 실행된다 — 삭제 중
         * 눌린 이벤트 추가가 삭제 직후에 열린다.
         */
        private fun deleteEvent() {
            val target = state.value.eventDeleteDialogState as? TimelineEventDeleteDialogState.Active ?: return
            if (target is TimelineEventDeleteDialogState.Deleting || eventDeleteJob?.isActive == true) return

            updateState {
                copy(eventDeleteDialogState = TimelineEventDeleteDialogState.Deleting(target.timelineEventId))
            }
            eventDeleteJob =
                safeLaunch(onError = { error -> handleEventDeleteFailure(target.timelineEventId, error) }) {
                    deleteTimelineEventUseCase(target.timelineEventId)
                        .onSuccess {
                            // 목록 갱신은 UseCase 가 세션에서 Event 를 빼는 것으로 이미 일어난다.
                            updateState { copy(eventDeleteDialogState = TimelineEventDeleteDialogState.Hidden) }
                        }.onFailure { error -> handleEventDeleteFailure(target.timelineEventId, error) }
                }
        }

        private fun dismissEventDelete() {
            if (state.value.eventDeleteDialogState is TimelineEventDeleteDialogState.Deleting) return
            updateState { copy(eventDeleteDialogState = TimelineEventDeleteDialogState.Hidden) }
        }

        private fun handleEventDeleteFailure(
            timelineEventId: Long,
            error: Throwable,
        ) {
            when (val action = error.toTimelineDeleteFailureAction()) {
                // 지우려던 이벤트가 이미 없는 경우다. 하루 기록은 멀쩡하므로 화면 전체를 닫지 않고
                // 목록만 서버 기준으로 되돌린다.
                TimelineDeleteFailureAction.TargetUnavailable -> {
                    updateState { copy(eventDeleteDialogState = TimelineEventDeleteDialogState.Hidden) }
                    sendEffect(TimelineRecordUiSideEffect.ShowSnackbar("이미 삭제됐거나 접근할 수 없는 이벤트예요."))
                    requestedRecordDate?.let(::loadRecord)
                }
                TimelineDeleteFailureAction.AlreadyHandled ->
                    updateState { copy(eventDeleteDialogState = TimelineEventDeleteDialogState.Hidden) }
                is TimelineDeleteFailureAction.Retryable ->
                    updateState {
                        copy(
                            eventDeleteDialogState =
                                TimelineEventDeleteDialogState.RetryableError(
                                    timelineEventId = timelineEventId,
                                    message = action.message,
                                ),
                        )
                    }
            }
        }

        private fun editMemo(timelineEventId: Long) {
            val current = state.value
            // 읽기 모드에서는 메모 영역이 눌리지 않지만 Intent 경로도 막는다.
            if (!current.mode.isEditing || !current.isModeSwitchable) return
            if (current.memoEditor?.timelineEventId == timelineEventId) return
            // 다른 메모를 누른 것이 곧 쓰던 메모에서 포커스가 빠진 것이다.
            commitOpenMemo()
            // 갈무리한 값을 얹은 뒤에 읽는다 — 방금 커밋한 메모를 다시 열면 그 글이 나와야 한다.
            val event =
                state.value
                    .displayedRecord
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
                copy(memoEditor = editor.copy(draftMemo = value))
            }
        }

        /**
         * 열려 있는 메모를 갈무리한다.
         *
         * 화면의 다른 동작이 자기 일을 하기 전에 부른다 — 무엇을 누르든 그 순간 메모에서 포커스는
         * 빠진다. 이 덕분에 메모를 쓰는 동안에도 화면이 굳지 않는다.
         */
        private fun commitOpenMemo() {
            state.value.memoEditor?.let { commitMemo(it.timelineEventId) }
        }

        /**
         * 포커스가 빠졌다 — 편집기를 닫고, 바뀐 게 있으면 뒤에서 저장한다.
         *
         * [timelineEventId] 가 지금 열린 편집기와 다르면 무시한다. 다른 메모로 편집이 옮겨 간 뒤에
         * 앞선 입력칸의 포커스 해제가 뒤늦게 도착할 수 있는데, 확인하지 않으면 방금 연 편집기를
         * 대신 닫는다.
         */
        private fun commitMemo(timelineEventId: Long) {
            val editor = state.value.memoEditor?.takeIf { it.timelineEventId == timelineEventId } ?: return
            updateState { copy(memoEditor = null) }
            if (!editor.hasChanges) return
            sendMemo(timelineEventId, editor.draftMemo.takeUnless(String::isBlank))
        }

        /**
         * 메모를 서버로 보낸다. 응답을 기다리며 화면을 묶지 않는다.
         *
         * 보낸 값을 [TimelineRecordUiState.pendingMemos] 에 얹는 것이 낙관 반영이자 되돌릴 자리다 —
         * 카드는 곧바로 쓴 글을 보여 주고, 응답을 기다리는 동안 세션이 다시 방출돼도 옛 값으로
         * 돌아가지 않는다.
         */
        private fun sendMemo(
            timelineEventId: Long,
            memo: String?,
        ) {
            val previous = memoCommitJobs[timelineEventId]
            updateState { copy(pendingMemos = pendingMemos + (timelineEventId to memo)) }
            memoCommitJobs[timelineEventId] =
                safeLaunch(onError = { error -> handleMemoCommitFailure(timelineEventId, memo, error) }) {
                    previous?.join()
                    updateTimelineEventMemoUseCase(
                        timelineEventId = timelineEventId,
                        memo = memo,
                    ).onSuccess {
                        // 성공하면 UseCase 가 세션에 같은 값을 넣어 두므로 덧씌울 것이 없다.
                        clearPendingMemo(timelineEventId)
                    }.onFailure { error -> handleMemoCommitFailure(timelineEventId, memo, error) }
                }
        }

        private fun clearPendingMemo(timelineEventId: Long) {
            updateState { copy(pendingMemos = pendingMemos - timelineEventId) }
        }

        /** 날아가는 중인 메모가 하루 기록 확정보다 늦게 서버에 닿지 않도록 기다린다. */
        private suspend fun awaitMemoCommits() {
            memoCommitJobs.values.toList().forEach { it.join() }
        }

        /**
         * 커밋 실패 — 낙관 반영을 되돌리고 알린다.
         *
         * 되돌리지 않으면 화면과 서버가 갈린 채로 남아, 다음 조회에서 사용자가 쓴 글이 소리 없이
         * 사라진다. 그때는 무엇을 잃었는지 알 방법이 없다.
         *
         * 편집기를 다시 열지는 않는다. 포커스는 이미 다른 곳에 가 있고, 되찾아 오면 지금 쓰던 글을
         * 방해한다. 대신 스낵바가 같은 값을 다시 보낼 길을 준다.
         */
        private fun handleMemoCommitFailure(
            timelineEventId: Long,
            memo: String?,
            error: Throwable,
        ) {
            clearPendingMemo(timelineEventId)
            if (error is CancellationException) return
            when ((error as? TimelineEventUpdateException)?.reason) {
                // 되돌릴 대상이 이미 없다. 목록을 서버 기준으로 맞춘다.
                TimelineEventUpdateException.Reason.EVENT_UNAVAILABLE -> {
                    showSnackbar("이미 삭제됐거나 접근할 수 없는 이벤트예요.")
                    requestedRecordDate?.let(::loadRecord)
                }
                // 같은 값을 다시 보내도 같은 답이 온다. 재시도를 권하지 않는다.
                TimelineEventUpdateException.Reason.INVALID_REQUEST -> showSnackbar("메모 내용을 다시 확인해 주세요.")
                // 메모 수정이 내는 사유는 위 둘뿐이다(`UpdateTimelineEventMemoUseCase`).
                // 나머지는 네트워크·서버 실패와 같은 자리에 둔다 — 다시 보내면 될 수 있는 것들이다.
                else -> {
                    if (error is HandledException) return
                    sendEffect(
                        TimelineRecordUiSideEffect.MemoCommitFailed(
                            timelineEventId = timelineEventId,
                            memo = memo,
                            message = "메모를 저장하지 못했어요.",
                        ),
                    )
                }
            }
        }

        private fun TimelineRecordUiState.record() = (content as? TimelineRecordUiContent.Record)?.value

        /** 저장 CTA 는 아직 저장하지 않은 기록에만 있다. SAVED 는 저장 API 를 다시 부르지 않는다. */
        private fun TimelineRecordUiState.unsavedRecord() = record()?.takeIf { !it.isSaved }
    }
