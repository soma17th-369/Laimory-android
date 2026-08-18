package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.exception.TimelineEventUpdateException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class UpdateTimelineEventMemoUseCaseTest {
    private class FakeRecordRepository(
        private val result: Result<TimelineEvent>,
    ) : TimelineRecordRepository {
        var requestedEventId: Long? = null
        var requestedMemo: String? = null

        override suspend fun getDailyRecords(): List<DailyTimeline> = error("사용하지 않음")

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline = error("사용하지 않음")

        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent = error("사용하지 않음")

        override suspend fun updateEventMemo(
            timelineEventId: Long,
            memo: String?,
        ): TimelineEvent {
            requestedEventId = timelineEventId
            requestedMemo = memo
            return result.getOrThrow()
        }

        override suspend fun deleteEvent(timelineEventId: Long) = Unit

        override suspend fun deleteEventPhoto(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = Unit

        override suspend fun saveDailyRecord(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) = error("사용하지 않음")

        override suspend fun deleteDailyRecord(recordDate: LocalDate) = Unit
    }

    private class FakeSessionRepository : TimelineRecordSessionRepository {
        override val timeline: StateFlow<DailyTimeline?> = MutableStateFlow(null)
        var replacedEvent: TimelineEvent? = null

        override fun save(timeline: DailyTimeline) = Unit

        override fun replaceEvent(event: TimelineEvent) {
            replacedEvent = event
        }

        override fun removeEvent(timelineEventId: Long) = Unit

        override fun removeEventItem(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = Unit

        override fun clear() = Unit
    }

    private class RecordingMessageHelper : MessageHelper {
        val messages = mutableListOf<UserMessage>()

        override fun send(message: UserMessage) {
            messages += message
        }
    }

    @Test
    fun `메모 수정 성공 응답을 세션에 교체한다`() =
        runBlocking {
            val event = event(memo = "수정한 메모")
            val repository = FakeRecordRepository(Result.success(event))
            val session = FakeSessionRepository()
            val useCase = UpdateTimelineEventMemoUseCase(repository, session, RecordingMessageHelper())

            val result = useCase(EVENT_ID, "수정한 메모")

            assertEquals(EVENT_ID, repository.requestedEventId)
            assertEquals("수정한 메모", repository.requestedMemo)
            assertEquals(event, result.getOrNull())
            assertEquals(event, session.replacedEvent)
        }

    @Test
    fun `메모 기능 오류는 화면 의미 오류로 변환한다`() =
        runBlocking {
            val cases =
                listOf(
                    -400 to TimelineEventUpdateException.Reason.INVALID_REQUEST,
                    -404 to TimelineEventUpdateException.Reason.EVENT_UNAVAILABLE,
                    -1003 to TimelineEventUpdateException.Reason.RECORD_ALREADY_SAVED,
                )

            cases.forEach { (errorCode, expectedReason) ->
                val helper = RecordingMessageHelper()
                val session = FakeSessionRepository()
                val exception = ApiException.ClientException(errorCode = errorCode, rawCode = 409)
                val useCase =
                    UpdateTimelineEventMemoUseCase(
                        FakeRecordRepository(Result.failure(exception)),
                        session,
                        helper,
                    )

                val failure = useCase(EVENT_ID, "메모").exceptionOrNull()

                assertTrue(failure is TimelineEventUpdateException)
                assertEquals(expectedReason, (failure as TimelineEventUpdateException).reason)
                assertTrue(helper.messages.isEmpty())
                assertEquals(null, session.replacedEvent)
            }
        }

    @Test
    fun `401은 BaseUseCase 세션 만료 정책을 유지한다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val exception = ApiException.UnauthorizedException(errorCode = -2001, rawCode = 401)
            val useCase =
                UpdateTimelineEventMemoUseCase(
                    FakeRecordRepository(Result.failure(exception)),
                    FakeSessionRepository(),
                    helper,
                )

            val failure = useCase(EVENT_ID, null).exceptionOrNull()

            assertTrue(failure is HandledException)
            assertEquals(listOf(UserMessage.SessionExpired), helper.messages)
        }

    private fun event(memo: String?) =
        TimelineEvent(
            timelineEventId = EVENT_ID,
            eventType = TimelineEventType.WORK,
            startAt = LocalDateTime.of(2026, 7, 8, 14, 0),
            endAt = null,
            title = "업무",
            subtitle = null,
            memo = memo,
            items = emptyList(),
        )

    private companion object {
        const val EVENT_ID = 17L
    }
}
