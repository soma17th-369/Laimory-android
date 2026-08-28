package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.exception.TimelineEventUpdateException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.CreateTimelineEventCommand
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class UpdateTimelineEventUseCaseTest {
    private class FakeRecordRepository(
        private val result: Result<TimelineEvent>,
        private val refreshed: DailyTimeline? = null,
    ) : TimelineRecordRepository {
        override suspend fun getDailyRecords(): List<DailyTimeline> = error("사용하지 않음")

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline = refreshed ?: error("재조회 실패")

        override suspend fun createEvent(command: CreateTimelineEventCommand): TimelineEvent = error("사용하지 않음")

        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent = result.getOrThrow()

        override suspend fun updateEventMemo(
            timelineEventId: Long,
            memo: String?,
        ): TimelineEvent = error("사용하지 않음")

        override suspend fun deleteEvent(timelineEventId: Long) = Unit

        override suspend fun deleteEventPhoto(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = Unit

        override suspend fun updateDailyRecordEmotion(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) = Unit

        override suspend fun saveDailyRecord(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) = error("사용하지 않음")

        override suspend fun getMonthlyDailyRecords(month: YearMonth): List<MonthlyDailyRecord> = error("사용하지 않음")

        override suspend fun deleteDailyRecord(recordDate: LocalDate) = Unit
    }

    private class FakeSessionRepository(
        current: DailyTimeline? = null,
    ) : TimelineRecordSessionRepository {
        override val timeline: StateFlow<DailyTimeline?> = MutableStateFlow(current)
        var saved: DailyTimeline? = null
        var replacedEvent: TimelineEvent? = null

        override fun save(timeline: DailyTimeline) {
            saved = timeline
        }

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
    fun `수정 성공 응답을 반환하고 세션을 서버에서 다시 읽는다`() =
        runBlocking {
            // 제자리 치환은 인덱스를 유지해서 시각을 고쳐도 목록 위치가 그대로다 — 서버가 정한
            // 순서를 그대로 받으려면 기록을 다시 읽어야 한다.
            val event = event(title = "수정됨")
            val session = FakeSessionRepository(current = timeline(events = emptyList()))
            val refreshed = timeline(events = listOf(event))
            val repository = FakeRecordRepository(Result.success(event), refreshed = refreshed)
            val useCase = UpdateTimelineEventUseCase(repository, session, RecordingMessageHelper())

            val result = useCase(command())

            assertEquals(event, result.getOrNull())
            assertEquals(refreshed, session.saved)
            assertNull(session.replacedEvent)
        }

    @Test
    fun `재조회가 실패해도 수정은 성공으로 남는다`() =
        runBlocking {
            // 서버 반영은 이미 끝났다. 재조회 실패로 편집을 되돌리면 같은 편집을 다시 하게 된다.
            val event = event(title = "수정됨")
            val session = FakeSessionRepository(current = timeline(events = emptyList()))
            val repository = FakeRecordRepository(Result.success(event), refreshed = null)
            val useCase = UpdateTimelineEventUseCase(repository, session, RecordingMessageHelper())

            val result = useCase(command())

            assertEquals(event, result.getOrNull())
            assertNull(session.saved)
        }

    @Test
    fun `기능 오류 코드는 화면이 처리할 의미 오류로 변환한다`() =
        runBlocking {
            val cases =
                listOf(
                    -400 to TimelineEventUpdateException.Reason.INVALID_REQUEST,
                    -1004 to TimelineEventUpdateException.Reason.PHOTO_LIMIT_EXCEEDED,
                    -404 to TimelineEventUpdateException.Reason.EVENT_UNAVAILABLE,
                    -1016 to TimelineEventUpdateException.Reason.DATE_OPERATION_IN_PROGRESS,
                )

            cases.forEach { (errorCode, expectedReason) ->
                val helper = RecordingMessageHelper()
                val session = FakeSessionRepository()
                val exception = ApiException.ClientException(errorCode = errorCode, rawCode = 404)
                val useCase = UpdateTimelineEventUseCase(FakeRecordRepository(Result.failure(exception)), session, helper)

                val failure = useCase(command()).exceptionOrNull()

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
                UpdateTimelineEventUseCase(
                    FakeRecordRepository(Result.failure(exception)),
                    FakeSessionRepository(),
                    helper,
                )

            val failure = useCase(command()).exceptionOrNull()

            assertTrue(failure is HandledException)
            assertEquals(listOf(UserMessage.SessionExpired), helper.messages)
        }

    private fun command() =
        UpdateTimelineEventCommand(
            timelineEventId = 17L,
            title = "수정 제목",
            subtitle = null,
            startAt = LocalDateTime.of(2026, 7, 8, 14, 0),
            endAt = null,
        )

    private fun timeline(events: List<TimelineEvent>) =
        DailyTimeline(
            dailyRecordId = 1L,
            recordDate = LocalDate.of(2026, 5, 8),
            emotion = null,
            events = events,
        )

    private fun event(title: String) =
        TimelineEvent(
            timelineEventId = 17L,
            eventType = TimelineEventType.WORK,
            startAt = LocalDateTime.of(2026, 7, 8, 14, 0),
            endAt = null,
            title = title,
            subtitle = null,
            memo = null,
            question = null,
            items = emptyList(),
        )
}
