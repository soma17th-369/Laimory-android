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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class CreateTimelineEventUseCaseTest {
    @Test
    fun `생성 성공하면 하루 기록을 다시 읽어 세션에 반영한다`() =
        runBlocking {
            // 응답 하나를 목록에 끼워 넣으면 클라이언트가 서버의 계산을 흉내 내는 셈이다.
            val created = event()
            val refreshed = timeline(events = listOf(created))
            val session = FakeSessionRepository(current = timeline(events = emptyList()))
            val useCase = useCase(FakeRepository(Result.success(created), refreshed), session)

            val result = useCase(command())

            assertEquals(created, result.getOrNull())
            assertEquals(refreshed, session.saved)
        }

    @Test
    fun `기능 오류 코드는 편집기가 다룰 수 있는 사유로 바꾼다`() =
        runBlocking {
            // 그냥 두면 404 가 공통 정책에서 HandledException 이 돼 편집기가 아무 반응 없이 남는다.
            val cases =
                listOf(
                    -400 to TimelineEventUpdateException.Reason.INVALID_REQUEST,
                    -1004 to TimelineEventUpdateException.Reason.PHOTO_LIMIT_EXCEEDED,
                    -404 to TimelineEventUpdateException.Reason.EVENT_UNAVAILABLE,
                    -1019 to TimelineEventUpdateException.Reason.DATE_OPERATION_IN_PROGRESS,
                )

            cases.forEach { (code, expected) ->
                val useCase =
                    useCase(FakeRepository(Result.failure(ApiException.ClientException(rawCode = 400, errorCode = code))))

                val error = useCase(command()).exceptionOrNull()

                assertEquals(expected, (error as? TimelineEventUpdateException)?.reason)
            }
        }

    @Test
    fun `모르는 오류는 공통 정책에 맡긴다`() =
        runBlocking {
            val useCase =
                useCase(FakeRepository(Result.failure(ApiException.ServerException(rawCode = 500, errorCode = -500))))

            val error = useCase(command()).exceptionOrNull()

            assertTrue(error is HandledException)
        }

    @Test
    fun `실패하면 세션을 건드리지 않는다`() =
        runBlocking {
            val session = FakeSessionRepository(current = timeline(events = emptyList()))
            val useCase =
                useCase(FakeRepository(Result.failure(ApiException.ClientException(rawCode = 404, errorCode = -404))), session)

            useCase(command())

            assertEquals(null, session.saved)
        }

    private fun useCase(
        repository: TimelineRecordRepository,
        session: TimelineRecordSessionRepository = FakeSessionRepository(),
    ) = CreateTimelineEventUseCase(repository, session, RecordingMessageHelper())

    private fun command() =
        CreateTimelineEventCommand(
            recordDate = RECORD_DATE,
            eventType = TimelineEventType.MEAL,
            title = "점심",
            subtitle = null,
            startAt = LocalDateTime.of(2026, 5, 8, 12, 30),
            endAt = null,
            memo = null,
        )

    private fun timeline(events: List<TimelineEvent>) =
        DailyTimeline(
            dailyRecordId = 1L,
            recordDate = RECORD_DATE,
            emotion = null,
            events = events,
        )

    private fun event() =
        TimelineEvent(
            timelineEventId = 42L,
            eventType = TimelineEventType.MEAL,
            startAt = LocalDateTime.of(2026, 5, 8, 12, 30),
            endAt = null,
            title = "점심",
            subtitle = null,
            memo = null,
            question = null,
            items = emptyList(),
        )

    private class FakeRepository(
        private val result: Result<TimelineEvent>,
        private val refreshed: DailyTimeline? = null,
    ) : TimelineRecordRepository {
        override suspend fun getDailyRecords(): List<DailyTimeline> = error("사용하지 않음")

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline = refreshed ?: error("재조회 실패")

        override suspend fun createEvent(command: CreateTimelineEventCommand): TimelineEvent = result.getOrThrow()

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

        override suspend fun updateDailyRecordEmotion(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) = error("사용하지 않음")

        override suspend fun saveDailyRecord(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) = error("사용하지 않음")

        override suspend fun getMonthlyDailyRecords(month: YearMonth): List<MonthlyDailyRecord> = error("사용하지 않음")

        override suspend fun deleteDailyRecord(recordDate: LocalDate) = error("사용하지 않음")
    }

    private class FakeSessionRepository(
        current: DailyTimeline? = null,
    ) : TimelineRecordSessionRepository {
        override val timeline: StateFlow<DailyTimeline?> = MutableStateFlow(current)
        var saved: DailyTimeline? = null

        override fun save(timeline: DailyTimeline) {
            saved = timeline
        }

        override fun replaceEvent(event: TimelineEvent) = Unit

        override fun removeEvent(timelineEventId: Long) = Unit

        override fun removeEventItem(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = Unit

        override fun clear() = Unit
    }

    private class RecordingMessageHelper : MessageHelper {
        override fun send(message: UserMessage) = Unit
    }

    private companion object {
        val RECORD_DATE: LocalDate = LocalDate.of(2026, 5, 8)
    }
}
