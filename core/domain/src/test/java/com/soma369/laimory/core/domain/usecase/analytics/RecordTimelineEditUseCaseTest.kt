package com.soma369.laimory.core.domain.usecase.analytics

import com.soma369.laimory.core.domain.model.analytics.AnalyticsTimelineEditLog
import com.soma369.laimory.core.domain.repository.AnalyticsTimelineEditLogRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecordTimelineEditUseCaseTest {
    private val date = LocalDate.parse("2026-09-21")
    private val repository = RecordingRepository()
    private val useCase = RecordTimelineEditUseCase(repository)

    @Test
    fun `고친 이벤트를 남긴다`() =
        runTest {
            useCase.edited(date, 1L)

            assertEquals(listOf(date to 1L), repository.edited)
        }

    @Test
    fun `질문이 붙은 이벤트를 지우면 AI 이벤트 삭제로 남긴다`() =
        runTest {
            useCase.deleted(date, 3L, question = "그때 누구와 있었나요?")

            assertEquals(listOf(date to 3L), repository.deletedAi)
        }

    @Test
    fun `질문이 없는 이벤트는 직접 추가한 것이라 AI 삭제로 세지 않는다`() =
        runTest {
            useCase.deleted(date, 4L, question = null)
            useCase.deleted(date, 5L, question = "  ")

            assertTrue(repository.deletedAi.isEmpty())
        }

    private class RecordingRepository : AnalyticsTimelineEditLogRepository {
        val edited = mutableListOf<Pair<LocalDate, Long>>()
        val deletedAi = mutableListOf<Pair<LocalDate, Long>>()

        override suspend fun markEdited(
            recordDate: LocalDate,
            timelineEventId: Long,
        ) {
            edited += recordDate to timelineEventId
        }

        override suspend fun markDeletedAi(
            recordDate: LocalDate,
            timelineEventId: Long,
        ) {
            deletedAi += recordDate to timelineEventId
        }

        override suspend fun take(recordDate: LocalDate): AnalyticsTimelineEditLog = AnalyticsTimelineEditLog.EMPTY

        override suspend fun clear() = Unit
    }
}
