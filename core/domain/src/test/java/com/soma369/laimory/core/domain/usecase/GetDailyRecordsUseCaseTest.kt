package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GetDailyRecordsUseCaseTest {
    private class FakeRecordRepository(
        private val result: Result<List<DailyTimeline>>,
    ) : TimelineRecordRepository {
        override suspend fun getDailyRecords(): List<DailyTimeline> = result.getOrThrow()

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
        ) = error("사용하지 않음")

        override suspend fun deleteDailyRecord(recordDate: LocalDate) = error("사용하지 않음")
    }

    private class RecordingMessageHelper : MessageHelper {
        val messages = mutableListOf<UserMessage>()

        override fun send(message: UserMessage) {
            messages += message
        }
    }

    @Test
    fun `전체 목록을 서버 순서 그대로 반환한다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val timelines = listOf(timeline(32L), timeline(31L))
            val useCase = GetDailyRecordsUseCase(FakeRecordRepository(Result.success(timelines)), helper)

            val result = useCase()

            assertEquals(timelines, result.getOrNull())
            assertTrue(helper.messages.isEmpty())
        }

    @Test
    fun `빈 목록은 성공으로 반환한다`() =
        runBlocking {
            val useCase =
                GetDailyRecordsUseCase(
                    FakeRecordRepository(Result.success(emptyList())),
                    RecordingMessageHelper(),
                )

            val result = useCase()

            assertEquals(emptyList<DailyTimeline>(), result.getOrNull())
        }

    @Test
    fun `401은 BaseUseCase 세션 만료 정책을 유지한다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val useCase =
                GetDailyRecordsUseCase(
                    FakeRecordRepository(
                        Result.failure(ApiException.UnauthorizedException(errorCode = -2001, rawCode = 401)),
                    ),
                    helper,
                )

            val result = useCase()

            assertTrue(result.exceptionOrNull() is HandledException)
            assertEquals(listOf(UserMessage.SessionExpired), helper.messages)
        }

    private fun timeline(id: Long) =
        DailyTimeline(
            dailyRecordId = id,
            recordDate = LocalDate.of(2026, 7, 27),
            emotion = null,
            events = emptyList(),
        )
}
