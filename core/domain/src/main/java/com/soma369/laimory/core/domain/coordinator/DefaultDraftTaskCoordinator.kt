package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.DraftTaskCompletion
import com.soma369.laimory.core.domain.model.timeline.DraftTaskPollingEvent
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatus
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatusOutcome
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.model.timeline.DraftTaskUnavailableReason
import com.soma369.laimory.core.domain.repository.ActiveDraftTaskRepository
import com.soma369.laimory.core.domain.usecase.GetDraftTaskStatusUseCase
import com.soma369.laimory.core.domain.usecase.ObserveDraftTaskUseCase
import com.soma369.laimory.core.domain.usecase.SaveTimelineRecordUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class DefaultDraftTaskCoordinator
    @Inject
    constructor(
        private val observeDraftTaskUseCase: ObserveDraftTaskUseCase,
        private val getDraftTaskStatusUseCase: GetDraftTaskStatusUseCase,
        private val activeTaskRepository: ActiveDraftTaskRepository,
        private val saveTimelineRecordUseCase: SaveTimelineRecordUseCase,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
        private val clock: Clock,
    ) : DraftTaskCoordinator {
        private val mutex = Mutex()
        private val mutableState = MutableStateFlow<DraftTaskTrackingState>(DraftTaskTrackingState.Idle)
        private val mutablePendingCompletion = MutableStateFlow<DraftTaskCompletion?>(null)

        /** 결과 저장과 완료 신호를 작업당 한 번으로 묶는 기준. */
        private var completedTaskId: String? = null
        private var activeTask: ActiveDraftTask? = null
        private var pollingJob: Job? = null
        private var completionRefreshJob: Job? = null
        private var pendingCompletionTaskId: String? = null
        private var isForeground = false
        private var hasRestored = false
        private var pauseAtLongRunning = true

        override val state: StateFlow<DraftTaskTrackingState> = mutableState.asStateFlow()

        override val pendingCompletion: StateFlow<DraftTaskCompletion?> = mutablePendingCompletion.asStateFlow()

        override suspend fun consumeCompletion(taskId: String): Boolean =
            mutex.withLock {
                if (mutablePendingCompletion.value?.taskId != taskId) return@withLock false
                mutablePendingCompletion.value = null
                true
            }

        override suspend fun start(
            taskId: String,
            recordDate: LocalDate,
        ) {
            val task = ActiveDraftTask(taskId = taskId, recordDate = recordDate, requestedAt = clock.instant())
            mutex.withLock {
                pollingJob?.cancel()
                pollingJob = null
                completionRefreshJob?.cancel()
                completionRefreshJob = null
                pendingCompletionTaskId = null
                completedTaskId = null
                mutablePendingCompletion.value = null
                activeTaskRepository.save(task)
                activeTask = task
                hasRestored = true
                pauseAtLongRunning = true
                mutableState.value = DraftTaskTrackingState.Processing(task)
                startPollingLocked(task)
            }
        }

        override suspend fun onForeground() {
            val completionTaskId =
                mutex.withLock {
                    isForeground = true
                    if (!hasRestored) {
                        activeTask = activeTaskRepository.get()
                        hasRestored = true
                    }
                    val task = activeTask ?: return@withLock null
                    val pendingTaskId = pendingCompletionTaskId
                    pendingCompletionTaskId = null
                    if (pendingTaskId == task.taskId) {
                        pendingTaskId
                    } else {
                        when (mutableState.value) {
                            DraftTaskTrackingState.Idle,
                            is DraftTaskTrackingState.Processing,
                            is DraftTaskTrackingState.RetryableError,
                            -> {
                                mutableState.value = DraftTaskTrackingState.Processing(task)
                                startPollingLocked(task)
                            }

                            is DraftTaskTrackingState.Failed,
                            is DraftTaskTrackingState.LongRunning,
                            is DraftTaskTrackingState.Success,
                            is DraftTaskTrackingState.Unavailable,
                            -> Unit
                        }
                        null
                    }
                }
            completionTaskId?.let(::refreshFromCompletionSignal)
        }

        override suspend fun onBackground() {
            mutex.withLock {
                isForeground = false
                pollingJob?.cancel()
                pollingJob = null
                completionRefreshJob?.cancel()
                completionRefreshJob = null
            }
        }

        override fun refreshFromCompletionSignal(taskId: String) {
            if (taskId.isBlank()) return
            applicationScope.launch {
                val refreshJob = coroutineContext.job
                val task =
                    mutex.withLock {
                        if (!hasRestored) {
                            activeTask = activeTaskRepository.get()
                            hasRestored = true
                        }
                        val currentTask = activeTask ?: return@launch
                        if (currentTask.taskId != taskId) return@launch
                        if (!isForeground) {
                            pendingCompletionTaskId = taskId
                            return@launch
                        }
                        if (completionRefreshJob?.isActive == true) return@launch
                        when (mutableState.value) {
                            DraftTaskTrackingState.Idle -> {
                                mutableState.value = DraftTaskTrackingState.Processing(currentTask)
                            }

                            is DraftTaskTrackingState.Processing,
                            is DraftTaskTrackingState.RetryableError,
                            is DraftTaskTrackingState.LongRunning,
                            -> Unit

                            is DraftTaskTrackingState.Failed,
                            is DraftTaskTrackingState.Success,
                            is DraftTaskTrackingState.Unavailable,
                            -> return@launch
                        }
                        pollingJob?.cancel()
                        pollingJob = null
                        completionRefreshJob = refreshJob
                        currentTask
                    }

                try {
                    val result = getDraftTaskStatusUseCase(taskId)
                    mutex.withLock {
                        if (!isForeground || activeTask?.taskId != task.taskId) return@withLock
                        val outcome =
                            result.getOrElse {
                                markRetryableErrorLocked(task)
                                return@withLock
                            }
                        handleOutcomeLocked(task, outcome)
                        if (mutableState.value is DraftTaskTrackingState.Processing) {
                            startPollingLocked(task)
                        }
                    }
                } finally {
                    mutex.withLock {
                        if (completionRefreshJob == refreshJob) completionRefreshJob = null
                    }
                }
            }
        }

        override fun retry() {
            applicationScope.launch {
                mutex.withLock {
                    val task = activeTask ?: return@withLock
                    if (mutableState.value !is DraftTaskTrackingState.RetryableError) return@withLock
                    mutableState.value = DraftTaskTrackingState.Processing(task)
                    startPollingLocked(task)
                }
            }
        }

        override fun continueWaiting() {
            applicationScope.launch {
                mutex.withLock {
                    val task = activeTask ?: return@withLock
                    if (mutableState.value !is DraftTaskTrackingState.LongRunning) return@withLock
                    pauseAtLongRunning = false
                    mutableState.value = DraftTaskTrackingState.Processing(task)
                    startPollingLocked(task)
                }
            }
        }

        override suspend fun discard() {
            mutex.withLock {
                pollingJob?.cancel()
                pollingJob = null
                completionRefreshJob?.cancel()
                completionRefreshJob = null
                pendingCompletionTaskId = null
                completedTaskId = null
                mutablePendingCompletion.value = null
                activeTask = null
                hasRestored = true
                pauseAtLongRunning = true
                activeTaskRepository.clear()
                mutableState.value = DraftTaskTrackingState.Idle
            }
        }

        private fun startPollingLocked(task: ActiveDraftTask) {
            if (!isForeground || pollingJob?.isActive == true) return
            val shouldPauseAtLongRunning = pauseAtLongRunning
            pollingJob =
                applicationScope.launch {
                    try {
                        observeDraftTaskUseCase(
                            taskId = task.taskId,
                            requestedAt = task.requestedAt,
                            pauseAtLongRunning = shouldPauseAtLongRunning,
                        ).collect { event -> handlePollingEvent(task, event) }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Throwable) {
                        markRetryableError(task)
                    }
                }
        }

        private suspend fun markRetryableError(task: ActiveDraftTask) {
            mutex.withLock {
                if (!isForeground || activeTask?.taskId != task.taskId) return@withLock
                mutableState.value = DraftTaskTrackingState.RetryableError(task)
            }
        }

        private suspend fun handlePollingEvent(
            task: ActiveDraftTask,
            event: DraftTaskPollingEvent,
        ) {
            mutex.withLock {
                if (!isForeground || activeTask?.taskId != task.taskId) return@withLock
                when (event) {
                    is DraftTaskPollingEvent.LongRunning ->
                        mutableState.value = DraftTaskTrackingState.LongRunning(task, event.elapsedSeconds)

                    is DraftTaskPollingEvent.RetryableFailure ->
                        mutableState.value = DraftTaskTrackingState.RetryableError(task)

                    is DraftTaskPollingEvent.Status -> handleOutcomeLocked(task, event.outcome)
                }
            }
        }

        private suspend fun handleOutcomeLocked(
            task: ActiveDraftTask,
            outcome: DraftTaskStatusOutcome,
        ) {
            when (outcome) {
                DraftTaskStatusOutcome.TaskUnavailable -> markUnavailableLocked(task, DraftTaskUnavailableReason.TASK)
                DraftTaskStatusOutcome.ResultUnavailable -> markUnavailableLocked(task, DraftTaskUnavailableReason.RESULT)
                is DraftTaskStatusOutcome.Snapshot -> {
                    val snapshot = outcome.value
                    when (snapshot.status) {
                        DraftTaskStatus.PROCESSING ->
                            mutableState.value = DraftTaskTrackingState.Processing(task, snapshot.elapsedSeconds)

                        DraftTaskStatus.SUCCESS -> {
                            val timeline =
                                snapshot.result
                                    ?: return markRetryableErrorLocked(task)
                            // FCM과 폴링이 같은 완료를 함께 감지하거나 같은 FCM이 재전달돼도
                            // 저장과 완료 신호는 작업당 한 번이다.
                            val isFirstCompletion = completedTaskId != task.taskId
                            if (isFirstCompletion) {
                                saveTimelineRecordUseCase(timeline)
                                completedTaskId = task.taskId
                            }
                            // 상태보다 먼저 채운다. 두 값을 함께 보는 쪽이 `Success` 인데 완료는
                            // 비어 있는 중간 상태를 보고 로딩 화면을 얹는 일이 없어야 한다.
                            if (isFirstCompletion) {
                                mutablePendingCompletion.value =
                                    DraftTaskCompletion(
                                        taskId = task.taskId,
                                        recordDate = timeline.recordDate,
                                    )
                            }
                            mutableState.value = DraftTaskTrackingState.Success(task)
                        }

                        DraftTaskStatus.FAILED ->
                            mutableState.value =
                                DraftTaskTrackingState.Failed(
                                    task = task,
                                    reason =
                                        snapshot.failure
                                            ?: return markRetryableErrorLocked(task),
                                )
                    }
                }
            }
        }

        private fun markRetryableErrorLocked(task: ActiveDraftTask) {
            mutableState.value = DraftTaskTrackingState.RetryableError(task)
        }

        private suspend fun markUnavailableLocked(
            task: ActiveDraftTask,
            reason: DraftTaskUnavailableReason,
        ) {
            activeTaskRepository.clear()
            activeTask = null
            mutableState.value = DraftTaskTrackingState.Unavailable(task, reason)
        }
    }
