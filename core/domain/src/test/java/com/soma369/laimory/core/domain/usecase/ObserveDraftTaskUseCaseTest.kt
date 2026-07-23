package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.DraftPollingPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftTaskFailureReason
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskPollingEvent
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatus
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatusOutcome
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveDraftTaskUseCaseTest {
    private val requestedAt = Instant.parse("2026-07-22T00:00:00Z")
    private val clock = Clock.fixed(requestedAt, ZoneId.of("UTC"))

    @Test
    fun `PROCESSING은 즉시 조회하고 5초 뒤 다시 조회한다`() =
        runTest {
            val repository = QueueRepository(mutableListOf(processing(), processing()))
            val events = mutableListOf<DraftTaskPollingEvent>()
            val job = launch { useCase(repository).invoke("task-1", requestedAt).take(2).toList(events) }

            runCurrent()
            assertEquals(1, repository.statusCallCount)

            advanceTimeBy(4_999)
            runCurrent()
            assertEquals(1, repository.statusCallCount)

            advanceTimeBy(1)
            runCurrent()
            assertEquals(2, repository.statusCallCount)
            assertEquals(2, events.size)
            job.cancel()
        }

    @Test
    fun `SUCCESS를 받으면 추가 조회 없이 종료한다`() =
        runTest {
            val timeline =
                DailyTimeline(
                    dailyRecordId = 1L,
                    recordDate = LocalDate.of(2026, 7, 22),
                    emotion = null,
                    events = emptyList(),
                )
            val repository = QueueRepository(mutableListOf(DraftTaskSnapshot(DraftTaskStatus.SUCCESS, result = timeline)))

            val events = useCase(repository).invoke("task-1", requestedAt).toList()

            assertEquals(1, repository.statusCallCount)
            assertTrue(events.single() is DraftTaskPollingEvent.Status)
            val status = events.single() as DraftTaskPollingEvent.Status
            assertEquals(DraftTaskStatus.SUCCESS, (status.outcome as DraftTaskStatusOutcome.Snapshot).value.status)
        }

    @Test
    fun `서버 경과 시간이 10분이면 장기 처리 이벤트를 내고 멈춘다`() =
        runTest {
            val repository = QueueRepository(mutableListOf(processing(elapsedSeconds = 600L)))

            val events = useCase(repository).invoke("task-1", requestedAt).toList()

            assertEquals(2, events.size)
            assertTrue(events[0] is DraftTaskPollingEvent.Status)
            assertEquals(DraftTaskPollingEvent.LongRunning(600L), events[1])
        }

    @Test
    fun `계속 대기에서는 장기 처리 경계를 건너뛴다`() =
        runTest {
            val repository =
                QueueRepository(
                    mutableListOf(
                        processing(elapsedSeconds = 600L),
                        DraftTaskSnapshot(DraftTaskStatus.FAILED, failure = DraftTaskFailureReason.UNKNOWN),
                    ),
                )

            val events =
                useCase(repository)
                    .invoke("task-1", requestedAt, pauseAtLongRunning = false)
                    .toList()

            assertEquals(2, events.size)
            assertEquals(2, repository.statusCallCount)
        }

    @Test
    fun `네트워크 오류는 재시도 가능 이벤트를 내고 멈춘다`() =
        runTest {
            val repository = QueueRepository(mutableListOf(ApiException.NetworkException()))

            val events = useCase(repository).invoke("task-1", requestedAt).toList()

            assertTrue(events.single() is DraftTaskPollingEvent.RetryableFailure)
            val failure = events.single() as DraftTaskPollingEvent.RetryableFailure
            assertTrue(failure.cause is ApiException.NetworkException)
            assertEquals(1, repository.statusCallCount)
        }

    private fun useCase(repository: TimelineDraftRepository) =
        ObserveDraftTaskUseCase(
            getDraftTaskStatusUseCase = GetDraftTaskStatusUseCase(repository, NoOpMessageHelper),
            policy = DraftPollingPolicy(),
            clock = clock,
        )

    private fun processing(elapsedSeconds: Long? = null) =
        DraftTaskSnapshot(status = DraftTaskStatus.PROCESSING, elapsedSeconds = elapsedSeconds)

    private class QueueRepository(
        private val responses: MutableList<Any>,
    ) : TimelineDraftRepository {
        var statusCallCount: Int = 0

        override suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot {
            statusCallCount++
            return when (val response = responses.removeFirst()) {
                is DraftTaskSnapshot -> response
                is Throwable -> throw response
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
}
