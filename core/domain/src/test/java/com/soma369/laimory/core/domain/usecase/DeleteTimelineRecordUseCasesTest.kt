package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.exception.TimelineRecordDeleteException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DeleteTimelineRecordUseCasesTest {
    @Test
    fun `Event 삭제 성공 뒤 해당 Event만 세션에서 제거한다`() =
        runBlocking {
            val repository = FakeRecordRepository()
            val session = FakeSessionRepository(timeline())
            val useCase = DeleteTimelineEventUseCase(repository, session, RecordingMessageHelper())

            val result = useCase(FIRST_EVENT_ID)

            assertTrue(result.isSuccess)
            assertEquals(FIRST_EVENT_ID, repository.deletedEventId)
            assertEquals(listOf(SECOND_EVENT_ID), session.timeline.value?.events?.map(TimelineEvent::timelineEventId))
            assertNotNull(session.timeline.value)
        }

    @Test
    fun `DailyRecord 삭제 성공 뒤 세션을 비운다`() =
        runBlocking {
            val repository = FakeRecordRepository()
            val session = FakeSessionRepository(timeline())
            val useCase = DeleteDailyRecordUseCase(repository, session, RecordingMessageHelper())

            val result = useCase(RECORD_DATE)

            assertTrue(result.isSuccess)
            assertEquals(RECORD_DATE, repository.deletedRecordDate)
            assertNull(session.timeline.value)
        }

    @Test
    fun `다른 DailyRecord 삭제 성공 뒤 현재 세션을 유지한다`() =
        runBlocking {
            val repository = FakeRecordRepository()
            val currentTimeline = timeline()
            val session = FakeSessionRepository(currentTimeline)
            val useCase = DeleteDailyRecordUseCase(repository, session, RecordingMessageHelper())

            val result = useCase(OTHER_RECORD_DATE)

            assertTrue(result.isSuccess)
            assertEquals(OTHER_RECORD_DATE, repository.deletedRecordDate)
            assertEquals(currentTimeline, session.timeline.value)
        }

    @Test
    fun `기능 오류 코드는 화면이 처리할 삭제 의미 오류로 변환하고 세션을 유지한다`() =
        runBlocking {
            val cases =
                listOf(
                    ApiException.ClientException(errorCode = -404, rawCode = 404) to
                        TimelineRecordDeleteException.Reason.TARGET_UNAVAILABLE,
                    ApiException.ConflictException(errorCode = -1003, rawCode = 409) to
                        TimelineRecordDeleteException.Reason.RECORD_ALREADY_SAVED,
                    ApiException.ConflictException(errorCode = -1016, rawCode = 409) to
                        TimelineRecordDeleteException.Reason.DATE_OPERATION_IN_PROGRESS,
                    ApiException.ServerException(errorCode = -1017, rawCode = 502) to
                        TimelineRecordDeleteException.Reason.PHOTO_DELETE_FAILED,
                )

            cases.forEach { (apiException, expectedReason) ->
                val helper = RecordingMessageHelper()
                val repository = FakeRecordRepository(failure = apiException)
                val session = FakeSessionRepository(timeline())
                val useCase = DeleteTimelineEventUseCase(repository, session, helper)

                val failure = useCase(FIRST_EVENT_ID).exceptionOrNull()

                assertTrue(failure is TimelineRecordDeleteException)
                assertEquals(expectedReason, (failure as TimelineRecordDeleteException).reason)
                assertEquals(listOf(FIRST_EVENT_ID, SECOND_EVENT_ID), session.timeline.value?.events?.map(TimelineEvent::timelineEventId))
                assertTrue(helper.messages.isEmpty())
            }
        }

    @Test
    fun `401은 BaseUseCase 세션 만료 정책을 유지한다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val repository =
                FakeRecordRepository(
                    failure = ApiException.UnauthorizedException(errorCode = -2001, rawCode = 401),
                )
            val useCase = DeleteDailyRecordUseCase(repository, FakeSessionRepository(timeline()), helper)

            val failure = useCase(RECORD_DATE).exceptionOrNull()

            assertTrue(failure is HandledException)
            assertEquals(listOf(UserMessage.SessionExpired), helper.messages)
        }

    private class FakeRecordRepository(
        private val failure: ApiException? = null,
    ) : TimelineRecordRepository {
        var deletedEventId: Long? = null
        var deletedRecordDate: LocalDate? = null

        override suspend fun getDailyRecords(): List<DailyTimeline> = error("사용하지 않음")

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline = error("사용하지 않음")

        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent = error("사용하지 않음")

        override suspend fun updateEventMemo(
            timelineEventId: Long,
            memo: String?,
        ): TimelineEvent = error("사용하지 않음")

        override suspend fun deleteEvent(timelineEventId: Long) {
            failure?.let { throw it }
            deletedEventId = timelineEventId
        }

        override suspend fun deleteEventPhoto(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = error("사용하지 않음")

        override suspend fun saveDailyRecord(recordDate: LocalDate) = error("사용하지 않음")

        override suspend fun deleteDailyRecord(recordDate: LocalDate) {
            failure?.let { throw it }
            deletedRecordDate = recordDate
        }
    }

    private class FakeSessionRepository(
        initial: DailyTimeline?,
    ) : TimelineRecordSessionRepository {
        private val mutableTimeline = MutableStateFlow(initial)
        override val timeline: StateFlow<DailyTimeline?> = mutableTimeline

        override fun save(timeline: DailyTimeline) {
            mutableTimeline.value = timeline
        }

        override fun replaceEvent(event: TimelineEvent) = Unit

        override fun removeEvent(timelineEventId: Long) {
            mutableTimeline.value =
                mutableTimeline.value?.copy(
                    events = mutableTimeline.value?.events.orEmpty().filterNot { it.timelineEventId == timelineEventId },
                )
        }

        override fun removeEventItem(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = Unit

        override fun clear() {
            mutableTimeline.value = null
        }
    }

    private class RecordingMessageHelper : MessageHelper {
        val messages = mutableListOf<UserMessage>()

        override fun send(message: UserMessage) {
            messages += message
        }
    }

    private fun timeline() =
        DailyTimeline(
            dailyRecordId = DAILY_RECORD_ID,
            recordDate = RECORD_DATE,
            emotion = null,
            events = listOf(event(FIRST_EVENT_ID), event(SECOND_EVENT_ID)),
        )

    private fun event(id: Long) =
        TimelineEvent(
            timelineEventId = id,
            eventType = TimelineEventType.WORK,
            startAt = LocalDateTime.of(2026, 7, 23, 9, 0),
            endAt = null,
            title = "업무",
            subtitle = null,
            memo = null,
            items = emptyList(),
        )

    private companion object {
        const val DAILY_RECORD_ID = 31L
        val RECORD_DATE: LocalDate = LocalDate.of(2026, 7, 23)
        val OTHER_RECORD_DATE: LocalDate = LocalDate.of(2026, 7, 24)
        const val FIRST_EVENT_ID = 17L
        const val SECOND_EVENT_ID = 18L
    }
}
