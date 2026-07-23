package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.DraftTaskPollingEvent
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatus
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatusOutcome
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.model.timeline.DraftTaskUnavailableReason
import com.soma369.laimory.core.domain.repository.ActiveDraftTaskRepository
import com.soma369.laimory.core.domain.usecase.ObserveDraftTaskUseCase
import com.soma369.laimory.core.domain.usecase.SaveTimelineRecordUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        private val activeTaskRepository: ActiveDraftTaskRepository,
        private val saveTimelineRecordUseCase: SaveTimelineRecordUseCase,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
        private val clock: Clock,
    ) : DraftTaskCoordinator {
        private val mutex = Mutex()
        private val mutableState = MutableStateFlow<DraftTaskTrackingState>(DraftTaskTrackingState.Idle)
        private var activeTask: ActiveDraftTask? = null
        private var pollingJob: Job? = null
        private var isForeground = false
        private var hasRestored = false
        private var pauseAtLongRunning = true

        override val state: StateFlow<DraftTaskTrackingState> = mutableState.asStateFlow()

        override suspend fun start(
            taskId: String,
            recordDate: LocalDate,
        ) {
            val task = ActiveDraftTask(taskId = taskId, recordDate = recordDate, requestedAt = clock.instant())
            mutex.withLock {
                pollingJob?.cancel()
                pollingJob = null
                activeTaskRepository.save(task)
                activeTask = task
                hasRestored = true
                pauseAtLongRunning = true
                mutableState.value = DraftTaskTrackingState.Processing(task)
                startPollingLocked(task)
            }
        }

        override suspend fun onForeground() {
            mutex.withLock {
                isForeground = true
                if (!hasRestored) {
                    activeTask = activeTaskRepository.get()
                    hasRestored = true
                }
                val task = activeTask ?: return@withLock
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
            }
        }

        override suspend fun onBackground() {
            mutex.withLock {
                isForeground = false
                pollingJob?.cancel()
                pollingJob = null
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
                            if (mutableState.value !is DraftTaskTrackingState.Success) {
                                saveTimelineRecordUseCase(timeline)
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
