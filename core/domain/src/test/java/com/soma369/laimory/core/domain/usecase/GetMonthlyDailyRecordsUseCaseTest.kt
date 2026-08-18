package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GetMonthlyDailyRecordsUseCaseTest {
    private val month: YearMonth = YearMonth.of(2026, 7)

    @Test
    fun `표시 월을 그대로 넘기고 서버 순서를 보존해 돌려준다`() =
        runBlocking {
            val records =
                listOf(
                    MonthlyDailyRecord(LocalDate.of(2026, 7, 1), TimelineEmotion.HAPPY),
                    MonthlyDailyRecord(LocalDate.of(2026, 7, 9), null),
                )
            val repository = FakeRepository(records = records)
            val useCase = GetMonthlyDailyRecordsUseCase(repository, RecordingMessageHelper())

            val result = useCase(month).getOrNull()

            assertEquals(records, result)
            assertEquals(listOf(month), repository.requestedMonths)
        }

    @Test
    fun `기록이 없는 월은 빈 목록으로 성공한다`() =
        runBlocking {
            val useCase = GetMonthlyDailyRecordsUseCase(FakeRepository(), RecordingMessageHelper())

            assertEquals(emptyList<MonthlyDailyRecord>(), useCase(month).getOrNull())
        }

    @Test
    fun `공통 정책에 걸리지 않는 실패는 화면이 처리하도록 그대로 내려간다`() =
        runBlocking {
            val failure = ApiException.NetworkException()
            val helper = RecordingMessageHelper()
            val useCase = GetMonthlyDailyRecordsUseCase(FakeRepository(failure = failure), helper)

            assertEquals(failure, useCase(month).exceptionOrNull())
            assertTrue(helper.sent.isEmpty())
        }

    @Test
    fun `세션 만료는 공통 정책으로 한 번만 알리고 처리 완료로 감싼다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val repository = FakeRepository(failure = ApiException.UnauthorizedException(rawCode = 401))
            val useCase = GetMonthlyDailyRecordsUseCase(repository, helper)

            assertTrue(useCase(month).exceptionOrNull() is HandledException)
            assertEquals(listOf(UserMessage.SessionExpired), helper.sent)
        }

    private class FakeRepository(
        private val records: List<MonthlyDailyRecord> = emptyList(),
        private val failure: ApiException? = null,
    ) : TimelineRecordRepository {
        val requestedMonths = mutableListOf<YearMonth>()

        override suspend fun getMonthlyDailyRecords(month: YearMonth): List<MonthlyDailyRecord> {
            requestedMonths += month
            failure?.let { throw it }
            return records
        }

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

        override suspend fun deleteDailyRecord(recordDate: LocalDate) = error("사용하지 않음")

        override suspend fun saveDailyRecord(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) = error("사용하지 않음")
    }

    private class RecordingMessageHelper : MessageHelper {
        val sent = mutableListOf<UserMessage>()

        override fun send(message: UserMessage) {
            sent += message
        }
    }
}
