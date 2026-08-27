package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.DailyRecordStatus
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CompleteDailyRecordUseCaseTest {
    private val recordDate: LocalDate = LocalDate.of(2026, 8, 12)
    private val emotion: TimelineEmotion = TimelineEmotion.HAPPY

    @Test
    fun `저장 성공은 Completed를 반환하고 같은 날짜의 세션을 비운다`() =
        runBlocking {
            val repository = FakeRecordRepository()
            val session = FakeSessionRepository(timeline(recordDate))
            val useCase = CompleteDailyRecordUseCase(repository, session, RecordingMessageHelper())

            val outcome = useCase(recordDate, emotion).getOrNull()

            assertEquals(CompleteDailyRecordOutcome.Completed, outcome)
            assertEquals(listOf(recordDate to emotion), repository.savedRecords)
            assertTrue(session.cleared)
        }

    @Test
    fun `다른 날짜의 세션은 비우지 않는다`() =
        runBlocking {
            val repository = FakeRecordRepository()
            val session = FakeSessionRepository(timeline(recordDate.plusDays(1)))
            val useCase = CompleteDailyRecordUseCase(repository, session, RecordingMessageHelper())

            useCase(recordDate, emotion)

            assertFalse(session.cleared)
        }

    @Test
    fun `이미 SAVED인 1003은 AlreadySaved로 수렴하고 세션을 비운다`() =
        runBlocking {
            val repository =
                FakeRecordRepository(
                    failure = ApiException.ConflictException(errorCode = -1003, rawCode = 409),
                )
            val session = FakeSessionRepository(timeline(recordDate))
            val useCase = CompleteDailyRecordUseCase(repository, session, RecordingMessageHelper())

            val outcome = useCase(recordDate, emotion).getOrNull()

            assertEquals(CompleteDailyRecordOutcome.AlreadySaved, outcome)
            assertTrue(session.cleared)
        }

    @Test
    fun `기록이 없는 404는 RecordUnavailable로 반환하고 세션을 비운다`() =
        runBlocking {
            val repository =
                FakeRecordRepository(
                    failure = ApiException.ClientException(errorCode = -404, rawCode = 404),
                )
            val session = FakeSessionRepository(timeline(recordDate))
            val useCase = CompleteDailyRecordUseCase(repository, session, RecordingMessageHelper())

            val outcome = useCase(recordDate, emotion).getOrNull()

            assertEquals(CompleteDailyRecordOutcome.RecordUnavailable, outcome)
            assertTrue(session.cleared)
        }

    @Test
    fun `네트워크 실패는 세션을 유지한 채 실패로 반환한다`() =
        runBlocking {
            val repository = FakeRecordRepository(failure = ApiException.NetworkException())
            val session = FakeSessionRepository(timeline(recordDate))
            val useCase = CompleteDailyRecordUseCase(repository, session, RecordingMessageHelper())

            val failure = useCase(recordDate, emotion).exceptionOrNull()

            assertTrue(failure is ApiException.NetworkException)
            assertFalse(session.cleared)
        }

    @Test
    fun `401은 BaseUseCase 세션 만료 정책을 유지한다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val repository =
                FakeRecordRepository(
                    failure = ApiException.UnauthorizedException(errorCode = -2001, rawCode = 401),
                )
            val useCase = CompleteDailyRecordUseCase(repository, FakeSessionRepository(null), helper)

            val failure = useCase(recordDate, emotion).exceptionOrNull()

            assertTrue(failure is HandledException)
            assertEquals(listOf(UserMessage.SessionExpired), helper.messages)
        }

    private fun timeline(recordDate: LocalDate) =
        DailyTimeline(
            dailyRecordId = 1L,
            recordDate = recordDate,
            emotion = null,
            events = emptyList(),
            status = DailyRecordStatus.DRAFT,
        )

    private class FakeRecordRepository(
        private val failure: Throwable? = null,
    ) : TimelineRecordRepository {
        val savedRecords = mutableListOf<Pair<LocalDate, TimelineEmotion>>()

        override suspend fun getDailyRecords(): List<DailyTimeline> = error("사용하지 않음")

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline = error("사용하지 않음")

        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent = error("사용하지 않음")

        override suspend fun updateEventMemo(
            timelineEventId: Long,
            memo: String?,
        ): TimelineEvent = error("사용하지 않음")

        override suspend fun deleteEvent(timelineEventId: Long) = error("사용하지 않음")

        override suspend fun deleteEventPhoto(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = error("사용하지 않음")

        override suspend fun saveDailyRecord(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) {
            savedRecords += recordDate to emotion
            failure?.let { throw it }
        }

        override suspend fun updateDailyRecordEmotion(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) = Unit

        override suspend fun getMonthlyDailyRecords(month: YearMonth): List<MonthlyDailyRecord> = error("사용하지 않음")

        override suspend fun deleteDailyRecord(recordDate: LocalDate) = error("사용하지 않음")
    }

    private class FakeSessionRepository(
        initial: DailyTimeline?,
    ) : TimelineRecordSessionRepository {
        override val timeline: StateFlow<DailyTimeline?> = MutableStateFlow(initial)
        var cleared = false

        override fun save(timeline: DailyTimeline) = Unit

        override fun replaceEvent(event: TimelineEvent) = Unit

        override fun removeEvent(timelineEventId: Long) = Unit

        override fun removeEventItem(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = Unit

        override fun clear() {
            cleared = true
        }
    }

    private class RecordingMessageHelper : MessageHelper {
        val messages = mutableListOf<UserMessage>()

        override fun send(message: UserMessage) {
            messages += message
        }
    }
}
