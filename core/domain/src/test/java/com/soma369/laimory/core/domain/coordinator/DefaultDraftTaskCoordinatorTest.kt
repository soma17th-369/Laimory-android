package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.DraftPollingPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatus
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.repository.ActiveDraftTaskRepository
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import com.soma369.laimory.core.domain.usecase.GetDraftTaskStatusUseCase
import com.soma369.laimory.core.domain.usecase.ObserveDraftTaskUseCase
import com.soma369.laimory.core.domain.usecase.SaveTimelineRecordUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDraftTaskCoordinatorTest {
    private val date = LocalDate.of(2026, 7, 22)
    private val now = Instant.parse("2026-07-22T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("UTC"))

    @Test
    fun `시작한 작업을 저장하고 SUCCESS 결과를 세션에 한 번만 저장한다`() =
        runTest {
            val draftRepository = QueueDraftRepository(processing(), success())
            val activeRepository = FakeActiveDraftTaskRepository()
            val sessionRepository = FakeTimelineRecordSessionRepository()
            val coordinator = coordinator(draftRepository, activeRepository, sessionRepository, backgroundScope)
            coordinator.onForeground()
            runCurrent()

            coordinator.start("task-1", date)
            runCurrent()

            assertEquals("task-1", activeRepository.current?.taskId)
            assertTrue(coordinator.state.value is DraftTaskTrackingState.Processing)
            assertEquals(1, draftRepository.statusCallCount)

            advanceTimeBy(60_000)
            runCurrent()

            val successState = coordinator.state.value as DraftTaskTrackingState.Success
            assertEquals(date, successState.task.recordDate)
            assertEquals(1, sessionRepository.saveCount)

            coordinator.onBackground()
            runCurrent()
            coordinator.onForeground()
            runCurrent()

            assertEquals(2, draftRepository.statusCallCount)
            assertEquals(1, sessionRepository.saveCount)
        }

    @Test
    fun `백그라운드에서는 polling을 멈추고 전경 복귀 즉시 다시 조회한다`() =
        runTest {
            val draftRepository = QueueDraftRepository(processing(), processing(), processing())
            val coordinator =
                coordinator(
                    draftRepository,
                    FakeActiveDraftTaskRepository(),
                    FakeTimelineRecordSessionRepository(),
                    backgroundScope,
                )
            coordinator.onForeground()
            runCurrent()
            coordinator.start("task-1", date)
            runCurrent()
            assertEquals(1, draftRepository.statusCallCount)

            coordinator.onBackground()
            runCurrent()
            advanceTimeBy(10_000)
            runCurrent()
            assertEquals(1, draftRepository.statusCallCount)

            coordinator.onForeground()
            runCurrent()
            assertEquals(2, draftRepository.statusCallCount)
        }

    @Test
    fun `프로세스 재생성 뒤 저장된 taskId를 복구해 즉시 조회한다`() =
        runTest {
            val activeTask = ActiveDraftTask("task-restored", date, now)
            val activeRepository = FakeActiveDraftTaskRepository(activeTask)
            val draftRepository = QueueDraftRepository(success())
            val sessionRepository = FakeTimelineRecordSessionRepository()
            val coordinator = coordinator(draftRepository, activeRepository, sessionRepository, backgroundScope)

            coordinator.onForeground()
            runCurrent()

            assertEquals(1, draftRepository.statusCallCount)
            assertTrue(coordinator.state.value is DraftTaskTrackingState.Success)
            assertEquals(1, sessionRepository.saveCount)
        }

    @Test
    fun `10분 장기 처리 후 계속 대기를 선택하면 같은 task를 이어서 조회한다`() =
        runTest {
            val draftRepository = QueueDraftRepository(processing(elapsedSeconds = 600L), success())
            val coordinator =
                coordinator(
                    draftRepository,
                    FakeActiveDraftTaskRepository(),
                    FakeTimelineRecordSessionRepository(),
                    backgroundScope,
                )
            coordinator.onForeground()
            runCurrent()
            coordinator.start("task-1", date)
            runCurrent()
            assertTrue(coordinator.state.value is DraftTaskTrackingState.LongRunning)

            coordinator.continueWaiting()
            runCurrent()

            assertEquals(2, draftRepository.statusCallCount)
            assertTrue(coordinator.state.value is DraftTaskTrackingState.Success)
        }

    @Test
    fun `계속 대기 선택은 같은 프로세스의 전경 복귀 후에도 유지한다`() =
        runTest {
            val draftRepository =
                QueueDraftRepository(
                    processing(elapsedSeconds = 600L),
                    processing(elapsedSeconds = 601L),
                    processing(elapsedSeconds = 602L),
                )
            val coordinator =
                coordinator(
                    draftRepository,
                    FakeActiveDraftTaskRepository(),
                    FakeTimelineRecordSessionRepository(),
                    backgroundScope,
                )
            coordinator.onForeground()
            coordinator.start("task-1", date)
            runCurrent()
            assertTrue(coordinator.state.value is DraftTaskTrackingState.LongRunning)

            coordinator.continueWaiting()
            runCurrent()
            coordinator.onBackground()
            coordinator.onForeground()
            runCurrent()

            assertEquals(3, draftRepository.statusCallCount)
            assertTrue(coordinator.state.value is DraftTaskTrackingState.Processing)
        }

    @Test
    fun `작업 소멸 응답은 영속 task를 지우고 unavailable 상태를 남긴다`() =
        runTest {
            val activeTask = ActiveDraftTask("task-missing", date, now)
            val activeRepository = FakeActiveDraftTaskRepository(activeTask)
            val draftRepository = QueueDraftRepository(TaskUnavailableResponse)
            val coordinator =
                coordinator(
                    draftRepository,
                    activeRepository,
                    FakeTimelineRecordSessionRepository(),
                    backgroundScope,
                )

            coordinator.onForeground()
            runCurrent()

            assertNull(activeRepository.current)
            assertTrue(coordinator.state.value is DraftTaskTrackingState.Unavailable)
        }

    @Test
    fun `작업 소멸 처리 중 저장소 정리가 실패하면 재시도 가능 상태로 전환한다`() =
        runTest {
            val activeTask = ActiveDraftTask("task-missing", date, now)
            val activeRepository =
                FakeActiveDraftTaskRepository(
                    initial = activeTask,
                    clearFailure = IOException("disk full"),
                )
            val coordinator =
                coordinator(
                    QueueDraftRepository(TaskUnavailableResponse),
                    activeRepository,
                    FakeTimelineRecordSessionRepository(),
                    backgroundScope,
                )

            coordinator.onForeground()
            runCurrent()

            assertEquals(activeTask, activeRepository.current)
            assertTrue(coordinator.state.value is DraftTaskTrackingState.RetryableError)
        }

    @Test
    fun `SUCCESS 결과가 계약과 다르면 앱을 종료하지 않고 재시도 가능 상태로 전환한다`() =
        runTest {
            val activeTask = ActiveDraftTask("task-invalid", date, now)
            val coordinator =
                coordinator(
                    QueueDraftRepository(DraftTaskSnapshot(status = DraftTaskStatus.SUCCESS)),
                    FakeActiveDraftTaskRepository(activeTask),
                    FakeTimelineRecordSessionRepository(),
                    backgroundScope,
                )

            coordinator.onForeground()
            runCurrent()

            assertTrue(coordinator.state.value is DraftTaskTrackingState.RetryableError)
        }

    @Test
    fun `현재 작업의 완료 신호는 polling 대기 없이 서버를 즉시 재조회한다`() =
        runTest {
            val draftRepository = QueueDraftRepository(processing(), success())
            val coordinator =
                coordinator(
                    draftRepository,
                    FakeActiveDraftTaskRepository(),
                    FakeTimelineRecordSessionRepository(),
                    backgroundScope,
                )
            coordinator.onForeground()
            coordinator.start("task-1", date)
            runCurrent()

            coordinator.refreshFromCompletionSignal("task-1")
            runCurrent()

            assertEquals(2, draftRepository.statusCallCount)
            assertTrue(coordinator.state.value is DraftTaskTrackingState.Success)
        }

    @Test
    fun `다른 작업의 완료 신호는 현재 상태를 변경하지 않는다`() =
        runTest {
            val draftRepository = QueueDraftRepository(processing(), success())
            val coordinator =
                coordinator(
                    draftRepository,
                    FakeActiveDraftTaskRepository(),
                    FakeTimelineRecordSessionRepository(),
                    backgroundScope,
                )
            coordinator.onForeground()
            coordinator.start("task-1", date)
            runCurrent()

            coordinator.refreshFromCompletionSignal("task-other")
            runCurrent()

            assertEquals(1, draftRepository.statusCallCount)
            assertTrue(coordinator.state.value is DraftTaskTrackingState.Processing)
        }

    @Test
    fun `백그라운드에서 받은 완료 신호는 전경 복귀 후 즉시 재조회한다`() =
        runTest {
            val draftRepository = QueueDraftRepository(processing(elapsedSeconds = 600L), success())
            val coordinator =
                coordinator(
                    draftRepository,
                    FakeActiveDraftTaskRepository(),
                    FakeTimelineRecordSessionRepository(),
                    backgroundScope,
                )
            coordinator.onForeground()
            coordinator.start("task-1", date)
            runCurrent()
            coordinator.onBackground()

            coordinator.refreshFromCompletionSignal("task-1")
            runCurrent()
            assertEquals(1, draftRepository.statusCallCount)

            coordinator.onForeground()
            runCurrent()

            assertEquals(2, draftRepository.statusCallCount)
            assertTrue(coordinator.state.value is DraftTaskTrackingState.Success)
        }

    @Test
    fun `프로세스 재생성 중 알림을 먼저 열어도 저장된 task와 대조해 전경에서 조회한다`() =
        runTest {
            val activeTask = ActiveDraftTask("task-restored", date, now)
            val draftRepository = QueueDraftRepository(success())
            val coordinator =
                coordinator(
                    draftRepository,
                    FakeActiveDraftTaskRepository(activeTask),
                    FakeTimelineRecordSessionRepository(),
                    backgroundScope,
                )

            coordinator.refreshFromCompletionSignal("task-restored")
            runCurrent()
            assertEquals(0, draftRepository.statusCallCount)

            coordinator.onForeground()
            runCurrent()

            assertEquals(1, draftRepository.statusCallCount)
            assertTrue(coordinator.state.value is DraftTaskTrackingState.Success)
        }

    private fun coordinator(
        draftRepository: TimelineDraftRepository,
        activeRepository: ActiveDraftTaskRepository,
        sessionRepository: TimelineRecordSessionRepository,
        scope: kotlinx.coroutines.CoroutineScope,
    ) = DefaultDraftTaskCoordinator(
        observeDraftTaskUseCase =
            ObserveDraftTaskUseCase(
                getDraftTaskStatusUseCase = GetDraftTaskStatusUseCase(draftRepository, NoOpMessageHelper),
                policy = DraftPollingPolicy(),
                clock = clock,
            ),
        getDraftTaskStatusUseCase = GetDraftTaskStatusUseCase(draftRepository, NoOpMessageHelper),
        activeTaskRepository = activeRepository,
        saveTimelineRecordUseCase = SaveTimelineRecordUseCase(sessionRepository),
        applicationScope = scope,
        clock = clock,
    )

    private fun processing(elapsedSeconds: Long? = null) =
        DraftTaskSnapshot(status = DraftTaskStatus.PROCESSING, elapsedSeconds = elapsedSeconds)

    private fun success() =
        DraftTaskSnapshot(
            status = DraftTaskStatus.SUCCESS,
            result = DailyTimeline(1L, date, emotion = null, events = emptyList()),
        )

    private class FakeActiveDraftTaskRepository(
        initial: ActiveDraftTask? = null,
        private val clearFailure: Throwable? = null,
    ) : ActiveDraftTaskRepository {
        private val state = MutableStateFlow(initial)
        val current: ActiveDraftTask? get() = state.value

        override fun observe(): Flow<ActiveDraftTask?> = state

        override suspend fun get(): ActiveDraftTask? = current

        override suspend fun save(task: ActiveDraftTask) {
            state.value = task
        }

        override suspend fun clear() {
            clearFailure?.let { throw it }
            state.value = null
        }
    }

    private class FakeTimelineRecordSessionRepository : TimelineRecordSessionRepository {
        private val mutableTimeline = MutableStateFlow<DailyTimeline?>(null)
        override val timeline: StateFlow<DailyTimeline?> = mutableTimeline
        var saveCount = 0

        override fun save(timeline: DailyTimeline) {
            saveCount++
            mutableTimeline.value = timeline
        }

        override fun replaceEvent(event: com.soma369.laimory.core.domain.model.timeline.TimelineEvent) = Unit

        override fun removeEvent(timelineEventId: Long) = Unit

        override fun removeEventItem(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = Unit

        override fun clear() {
            mutableTimeline.value = null
        }
    }

    private class QueueDraftRepository(
        vararg responses: Any,
    ) : TimelineDraftRepository {
        private val responses = ArrayDeque(responses.toList())
        var statusCallCount = 0

        override suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot {
            statusCallCount++
            return when (val response = responses.removeFirst()) {
                is DraftTaskSnapshot -> response
                TaskUnavailableResponse ->
                    throw com.soma369.laimory.core.domain.exception.ApiException.ClientException(
                        errorCode = -1001,
                        rawCode = 404,
                    )

                else -> error("지원하지 않는 응답입니다.")
            }
        }

        override suspend fun uploadPhotos(clientPhotoUris: List<String>): List<String> = error("사용하지 않음")

        override suspend fun createDraft(
            recordDate: LocalDate,
            zone: ZoneId,
            window: RecordDateWindow,
            items: List<SourceItem>,
            uploadedPhotoFilenames: Map<String, String>,
        ): DraftTaskHandle = error("사용하지 않음")
    }

    private data object NoOpMessageHelper : MessageHelper {
        override fun send(message: UserMessage) = Unit
    }

    private data object TaskUnavailableResponse
}
