package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.DailyRecordReadOutcome
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GetDailyRecordUseCaseTest {
    private class FakeRecordRepository(
        private val result: Result<DailyTimeline>,
    ) : TimelineRecordRepository {
        var requestedRecordDate: LocalDate? = null

        override suspend fun getDailyRecords(): List<DailyTimeline> = error("사용하지 않음")

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline {
            requestedRecordDate = recordDate
            return result.getOrThrow()
        }

        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent = error("사용하지 않음")

        override suspend fun updateEventMemo(
            timelineEventId: Long,
            memo: String?,
        ): TimelineEvent = error("사용하지 않음")

        override suspend fun deleteEvent(timelineEventId: Long) = error("사용하지 않음")

        override suspend fun deleteDailyRecord(recordDate: LocalDate) = error("사용하지 않음")
    }

    private class RecordingMessageHelper : MessageHelper {
        val messages = mutableListOf<UserMessage>()

        override fun send(message: UserMessage) {
            messages += message
        }
    }

    @Test
    fun `조회 성공은 Record outcome으로 반환한다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val repository = FakeRecordRepository(Result.success(timeline()))
            val useCase = GetDailyRecordUseCase(repository, helper)

            val result = useCase(RECORD_DATE)

            assertEquals(DailyRecordReadOutcome.Record(timeline()), result.getOrNull())
            assertEquals(RECORD_DATE, repository.requestedRecordDate)
            assertTrue(helper.messages.isEmpty())
        }

    @Test
    fun `-404는 기록 소멸 outcome으로 변환한다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val useCase =
                GetDailyRecordUseCase(
                    FakeRecordRepository(
                        Result.failure(ApiException.ClientException(errorCode = -404, rawCode = 404)),
                    ),
                    helper,
                )

            val result = useCase(RECORD_DATE)

            assertEquals(DailyRecordReadOutcome.Unavailable, result.getOrNull())
            assertTrue(helper.messages.isEmpty())
        }

    @Test
    fun `미지 404는 기존 BaseUseCase 공통 정책으로 처리한다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val useCase =
                GetDailyRecordUseCase(
                    FakeRecordRepository(
                        Result.failure(ApiException.ClientException(errorCode = -1999, rawCode = 404)),
                    ),
                    helper,
                )

            val result = useCase(RECORD_DATE)

            assertTrue(result.exceptionOrNull() is HandledException)
            assertEquals(listOf(UserMessage.UnsupportedFeature), helper.messages)
        }

    @Test
    fun `401은 BaseUseCase 세션 만료 정책을 유지한다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val useCase =
                GetDailyRecordUseCase(
                    FakeRecordRepository(
                        Result.failure(ApiException.UnauthorizedException(errorCode = -2001, rawCode = 401)),
                    ),
                    helper,
                )

            val result = useCase(RECORD_DATE)

            assertTrue(result.exceptionOrNull() is HandledException)
            assertEquals(listOf(UserMessage.SessionExpired), helper.messages)
        }

    private fun timeline() =
        DailyTimeline(
            dailyRecordId = DAILY_RECORD_ID,
            recordDate = RECORD_DATE,
            emotion = null,
            events = emptyList(),
        )

    private companion object {
        const val DAILY_RECORD_ID = 31L
        val RECORD_DATE: LocalDate = LocalDate.of(2026, 7, 27)
    }
}
