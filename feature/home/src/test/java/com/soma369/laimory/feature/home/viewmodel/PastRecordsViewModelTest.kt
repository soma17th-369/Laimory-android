package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.CreateTimelineEventCommand
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.usecase.GetDailyRecordsUseCase
import com.soma369.laimory.feature.home.state.PastRecordsContent
import com.soma369.laimory.feature.home.state.PastRecordsUiIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class PastRecordsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recordRepository = FakeRecordRepository()
    private val navigationHelper = RecordingNavigationHelper()

    @Test
    fun `서버 정렬을 보존한 채 달로 나누고 최신 달을 위에 둔다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.dailyRecords =
                listOf(
                    timeline(id = 33L, date = LocalDate.of(2026, 7, 27)),
                    timeline(id = 32L, date = LocalDate.of(2026, 7, 26)),
                    timeline(id = 31L, date = LocalDate.of(2026, 6, 30)),
                )
            val viewModel = createViewModel()

            viewModel.sendIntent(PastRecordsUiIntent.Sync)
            runCurrent()

            val months = (viewModel.state.value.content as PastRecordsContent.Groups).months
            assertEquals(listOf(YearMonth.of(2026, 7), YearMonth.of(2026, 6)), months.map { it.yearMonth })
            // 달 안의 순서는 서버가 정한 그대로다.
            assertEquals(listOf(33L, 32L), months.first().records.map { it.dailyRecordId })
            assertEquals(listOf(31L), months.last().records.map { it.dailyRecordId })
        }

    @Test
    fun `기록이 없으면 빈 상태를 표시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(PastRecordsUiIntent.Sync)
            runCurrent()

            assertEquals(PastRecordsContent.Empty, viewModel.state.value.content)
        }

    @Test
    fun `조회에 실패하면 다시 시도로 복구한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.failure = ApiException.NetworkException()
            val viewModel = createViewModel()

            viewModel.sendIntent(PastRecordsUiIntent.Sync)
            runCurrent()
            assertEquals(PastRecordsContent.LoadFailed, viewModel.state.value.content)

            recordRepository.failure = null
            recordRepository.dailyRecords = listOf(timeline(id = 32L))
            viewModel.sendIntent(PastRecordsUiIntent.Sync)
            runCurrent()

            assertTrue(viewModel.state.value.content is PastRecordsContent.Groups)
        }

    @Test
    fun `보여 주던 목록이 있으면 갱신에 실패해도 지우지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 갱신에 실패했을 뿐 목록은 아직 유효하다. 지우면 복귀할 때마다 화면이 비어 보인다.
            recordRepository.dailyRecords = listOf(timeline(id = 32L))
            val viewModel = createViewModel()
            viewModel.sendIntent(PastRecordsUiIntent.Sync)
            runCurrent()

            recordRepository.failure = ApiException.NetworkException()
            viewModel.sendIntent(PastRecordsUiIntent.Sync)
            runCurrent()

            assertTrue(viewModel.state.value.content is PastRecordsContent.Groups)
        }

    @Test
    fun `동기화 중 중복 요청을 보내지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.gate = CompletableDeferred()
            val viewModel = createViewModel()

            viewModel.sendIntent(PastRecordsUiIntent.Sync)
            viewModel.sendIntent(PastRecordsUiIntent.Sync)
            runCurrent()

            assertEquals(1, recordRepository.callCount)

            recordRepository.gate?.complete(listOf(timeline(id = 32L)))
            runCurrent()

            assertTrue(viewModel.state.value.content is PastRecordsContent.Groups)
        }

    @Test
    fun `기록을 고르면 그 날짜의 타임라인 화면으로 간다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val recordDate = LocalDate.of(2026, 7, 27)
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(PastRecordsUiIntent.SelectRecord(recordDate))
            runCurrent()

            assertEquals(listOf<Page>(TimelinePage(recordDate)), navigationHelper.destinations)
        }

    private fun createViewModel(): PastRecordsViewModel =
        PastRecordsViewModel(
            getDailyRecordsUseCase = GetDailyRecordsUseCase(recordRepository, NoOpMessageHelper),
            navigationHelper = navigationHelper,
        )

    private fun timeline(
        id: Long,
        date: LocalDate = LocalDate.of(2026, 7, 27),
    ) = DailyTimeline(
        dailyRecordId = id,
        recordDate = date,
        emotion = TimelineEmotion.HAPPY,
        events =
            listOf(
                TimelineEvent(
                    timelineEventId = id,
                    eventType = TimelineEventType.MEAL,
                    startAt = LocalDateTime.of(date, java.time.LocalTime.NOON),
                    endAt = null,
                    title = "점심",
                    subtitle = "파스타",
                    memo = null,
                    question = null,
                    items = emptyList(),
                ),
            ),
    )

    private class FakeRecordRepository : TimelineRecordRepository {
        var dailyRecords: List<DailyTimeline> = emptyList()
        var gate: CompletableDeferred<List<DailyTimeline>>? = null
        var failure: ApiException? = null
        var callCount = 0

        override suspend fun getDailyRecords(): List<DailyTimeline> {
            callCount++
            failure?.let { throw it }
            return gate?.await() ?: dailyRecords
        }

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline = error("사용하지 않음")

        override suspend fun createEvent(command: CreateTimelineEventCommand): TimelineEvent = error("사용하지 않음")

        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent = error("사용하지 않음")

        override suspend fun updateEventMemo(
            timelineEventId: Long,
            memo: String?,
        ) = error("사용하지 않음")

        override suspend fun deleteEvent(timelineEventId: Long) = error("사용하지 않음")

        override suspend fun deleteEventPhoto(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = error("사용하지 않음")

        override suspend fun deleteDailyRecord(recordDate: LocalDate) = error("사용하지 않음")

        override suspend fun getMonthlyDailyRecords(month: YearMonth): List<MonthlyDailyRecord> = error("사용하지 않음")

        override suspend fun updateDailyRecordEmotion(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) = error("사용하지 않음")

        override suspend fun saveDailyRecord(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) = error("사용하지 않음")
    }

    private data object NoOpMessageHelper : MessageHelper {
        override fun send(message: UserMessage) = Unit
    }

    private class RecordingNavigationHelper : NavigationHelper {
        val destinations = mutableListOf<Page>()

        override fun navigateTo(page: Page) {
            destinations += page
        }

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() = Unit
    }
}
