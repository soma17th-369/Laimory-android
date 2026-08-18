package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.usecase.GetDailyRecordsUseCase
import com.soma369.laimory.core.ui.theme.Emotion
import com.soma369.laimory.feature.timeline.state.CalendarRecordsUiContent
import com.soma369.laimory.feature.timeline.state.CalendarUiIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = RecordingDailyRecordsRepository()
    private val messageHelper = RecordingUserMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()
    private val clock = MutableClock(BASE_INSTANT)

    private var originalTimeZone: TimeZone? = null

    @Before
    fun setUp() {
        // 기기 현지 시간대 계산을 검증해야 하므로 JVM 기본 시간대를 고정한다.
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(SEOUL))
    }

    @After
    fun tearDown() {
        originalTimeZone?.let(TimeZone::setDefault)
    }

    @Test
    fun `최초 진입은 UTC 가 아니라 기기 시간대의 오늘을 선택하고 그 달을 표시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            // BASE_INSTANT 는 UTC 로 5월 25일이지만 서울에서는 이미 5월 26일이다.
            assertEquals(TODAY, viewModel.state.value.today)
            assertEquals(TODAY, viewModel.state.value.selectedDate)
            assertEquals(YearMonth.of(2026, 5), viewModel.state.value.visibleMonth)
        }

    @Test
    fun `동기화 성공은 날짜별 대표 기록을 상태에 담는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.records = listOf(timeline(1L, TODAY, TimelineEmotion.VERY_HAPPY))
            val viewModel = createViewModel()

            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            val content = viewModel.state.value.content as CalendarRecordsUiContent.Content
            assertEquals(setOf(TODAY), content.recordsByDate.keys)
            assertEquals(Emotion.JOY, viewModel.state.value.recordOf(TODAY)?.emotion)
        }

    @Test
    fun `서버 전체 응답이 비어 있을 때만 전체 Empty 로 내려간다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.records = emptyList()
            val viewModel = createViewModel()

            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(CalendarRecordsUiContent.Empty, viewModel.state.value.content)
        }

    @Test
    fun `기록이 없는 월로 이동해도 Content 를 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.records = listOf(timeline(1L, TODAY, TimelineEmotion.HAPPY))
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            viewModel.sendIntent(CalendarUiIntent.ShowMonth(YearMonth.of(2026, 6)))
            advanceUntilIdle()

            assertTrue(viewModel.state.value.content is CalendarRecordsUiContent.Content)
            assertEquals(YearMonth.of(2026, 6), viewModel.state.value.visibleMonth)
            assertNull(viewModel.state.value.recordOf(LocalDate.of(2026, 6, 1)))
        }

    @Test
    fun `최초 조회 실패는 전체 오류 상태로 표시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.failure = ApiException.NetworkException()
            val viewModel = createViewModel()

            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(CalendarRecordsUiContent.LoadFailed, viewModel.state.value.content)
        }

    @Test
    fun `이미 격자를 보여주는 중이면 재동기화 실패로 지우지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.records = listOf(timeline(1L, TODAY, TimelineEmotion.HAPPY))
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            repository.failure = ApiException.NetworkException()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            val content = viewModel.state.value.content as CalendarRecordsUiContent.Content
            assertEquals(setOf(TODAY), content.recordsByDate.keys)
        }

    @Test
    fun `조회 실패 후 다시 시도는 재조회한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.failure = ApiException.NetworkException()
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            repository.failure = null
            repository.records = listOf(timeline(1L, TODAY, TimelineEmotion.NEUTRAL))
            viewModel.sendIntent(CalendarUiIntent.RetryLoad)
            advanceUntilIdle()

            assertEquals(2, repository.callCount)
            assertEquals(Emotion.MELLOW, viewModel.state.value.recordOf(TODAY)?.emotion)
        }

    @Test
    fun `진행 중인 동기화가 있으면 중복 요청하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<List<DailyTimeline>>()
            repository.gate = gate
            val viewModel = createViewModel()

            viewModel.sendIntent(CalendarUiIntent.Sync)
            runCurrent()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            runCurrent()
            assertEquals(1, repository.callCount)

            gate.complete(emptyList())
            advanceUntilIdle()
            assertEquals(1, repository.callCount)
        }

    @Test
    fun `월 전환은 과거·미래 제한 없이 이동하고 선택 날짜를 바꾸지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(CalendarUiIntent.ShowMonth(YearMonth.of(1901, 3)))
            advanceUntilIdle()
            assertEquals(YearMonth.of(1901, 3), viewModel.state.value.visibleMonth)

            viewModel.sendIntent(CalendarUiIntent.ShowMonth(YearMonth.of(2999, 12)))
            advanceUntilIdle()
            assertEquals(YearMonth.of(2999, 12), viewModel.state.value.visibleMonth)

            assertEquals(TODAY, viewModel.state.value.selectedDate)
        }

    @Test
    fun `기록이 있는 날짜를 선택하면 단건 조회 화면으로 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.records = listOf(timeline(1L, TODAY, TimelineEmotion.HAPPY))
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            viewModel.sendIntent(CalendarUiIntent.SelectDate(TODAY))
            advanceUntilIdle()

            assertEquals(listOf<Page>(TimelinePage(TODAY)), navigationHelper.pages)
            assertEquals(TODAY, viewModel.state.value.selectedDate)
        }

    @Test
    fun `기록이 없는 날짜는 선택만 갱신하고 이동하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.records = listOf(timeline(1L, TODAY, TimelineEmotion.HAPPY))
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            val emptyDate = TODAY.minusDays(3)
            viewModel.sendIntent(CalendarUiIntent.SelectDate(emptyDate))
            advanceUntilIdle()

            assertTrue(navigationHelper.pages.isEmpty())
            assertEquals(emptyDate, viewModel.state.value.selectedDate)
        }

    @Test
    fun `재동기화로 선택 날짜의 기록이 사라지면 빈 날짜가 된다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.records = listOf(timeline(1L, TODAY, TimelineEmotion.HAPPY))
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            repository.records = emptyList()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(TODAY, viewModel.state.value.selectedDate)
            assertNull(viewModel.state.value.recordOf(TODAY))

            // 기록이 사라진 뒤에는 같은 날짜를 눌러도 이동하지 않는다.
            viewModel.sendIntent(CalendarUiIntent.SelectDate(TODAY))
            advanceUntilIdle()
            assertTrue(navigationHelper.pages.isEmpty())
        }

    @Test
    fun `복귀 시 자정을 넘겼으면 오늘을 다시 계산한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            assertEquals(TODAY, viewModel.state.value.today)

            // 서울 기준 2026-06-01 00:30.
            clock.current = Instant.parse("2026-05-31T15:30:00Z")
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(LocalDate.of(2026, 6, 1), viewModel.state.value.today)
            // 오늘이 바뀌어도 표시 월과 선택 날짜는 사용자 조작으로만 바뀐다.
            assertEquals(YearMonth.of(2026, 5), viewModel.state.value.visibleMonth)
            assertEquals(TODAY, viewModel.state.value.selectedDate)
        }

    @Test
    fun `연·월 피커는 표시 중인 월의 연도로 열린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.ShowMonth(YearMonth.of(2025, 12)))
            advanceUntilIdle()
            assertEquals(YearMonth.of(2025, 12), viewModel.state.value.visibleMonth)

            viewModel.sendIntent(CalendarUiIntent.OpenMonthPicker)
            advanceUntilIdle()

            assertEquals(2025, viewModel.state.value.monthPicker?.year)
        }

    @Test
    fun `피커 연도만 넘기면 표시 월은 그대로다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.OpenMonthPicker)
            advanceUntilIdle()

            repeat(3) { viewModel.sendIntent(CalendarUiIntent.ShowNextPickerYear) }
            viewModel.sendIntent(CalendarUiIntent.ShowPreviousPickerYear)
            advanceUntilIdle()

            assertEquals(2028, viewModel.state.value.monthPicker?.year)
            assertEquals(YearMonth.of(2026, 5), viewModel.state.value.visibleMonth)
        }

    @Test
    fun `월을 고르면 표시 월을 옮기고 피커를 닫는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.OpenMonthPicker)
            viewModel.sendIntent(CalendarUiIntent.ShowPreviousPickerYear)
            advanceUntilIdle()

            viewModel.sendIntent(CalendarUiIntent.SelectMonth(YearMonth.of(2025, 11)))
            advanceUntilIdle()

            assertEquals(YearMonth.of(2025, 11), viewModel.state.value.visibleMonth)
            assertNull(viewModel.state.value.monthPicker)
            // 월 전환은 스와이프와 마찬가지로 선택 날짜를 바꾸지 않는다.
            assertEquals(TODAY, viewModel.state.value.selectedDate)
        }

    @Test
    fun `피커를 그냥 닫으면 넘겨보던 연도는 버려진다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.OpenMonthPicker)
            viewModel.sendIntent(CalendarUiIntent.ShowNextPickerYear)
            advanceUntilIdle()

            viewModel.sendIntent(CalendarUiIntent.DismissMonthPicker)
            advanceUntilIdle()

            assertNull(viewModel.state.value.monthPicker)
            assertEquals(YearMonth.of(2026, 5), viewModel.state.value.visibleMonth)

            // 다시 열면 넘겨보던 연도가 아니라 표시 월의 연도에서 시작한다.
            viewModel.sendIntent(CalendarUiIntent.OpenMonthPicker)
            advanceUntilIdle()
            assertEquals(2026, viewModel.state.value.monthPicker?.year)
        }

    @Test
    fun `피커가 닫혀 있으면 연도 이동 intent 를 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(CalendarUiIntent.ShowNextPickerYear)
            advanceUntilIdle()

            assertNull(viewModel.state.value.monthPicker)
        }

    private fun createViewModel() =
        CalendarViewModel(
            getDailyRecordsUseCase = GetDailyRecordsUseCase(repository, messageHelper),
            navigationHelper = navigationHelper,
            clock = clock,
        )

    private fun timeline(
        id: Long,
        date: LocalDate,
        emotion: TimelineEmotion?,
    ) = DailyTimeline(
        dailyRecordId = id,
        recordDate = date,
        emotion = emotion,
        events = emptyList(),
    )

    private class RecordingDailyRecordsRepository : TimelineRecordRepository {
        var records: List<DailyTimeline> = emptyList()
        var failure: ApiException? = null
        var gate: CompletableDeferred<List<DailyTimeline>>? = null
        var callCount = 0

        override suspend fun getDailyRecords(): List<DailyTimeline> {
            callCount++
            gate?.let { pending ->
                gate = null
                return pending.await()
            }
            failure?.let { throw it }
            return records
        }

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

    private class RecordingUserMessageHelper : MessageHelper {
        val sent = mutableListOf<UserMessage>()

        override fun send(message: UserMessage) {
            sent += message
        }
    }

    private class RecordingNavigationHelper : NavigationHelper {
        val pages = mutableListOf<Page>()

        override fun navigateTo(page: Page) {
            pages += page
        }

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() = Unit
    }

    /** 자정 경과를 재현하기 위한 조작 가능한 시계. 주입되는 실제 Clock 처럼 UTC 기준이다. */
    private class MutableClock(
        var current: Instant,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("UTC")

        override fun withZone(zone: ZoneId): Clock =
            object : Clock() {
                override fun getZone(): ZoneId = zone

                override fun withZone(other: ZoneId): Clock = this@MutableClock.withZone(other)

                override fun instant(): Instant = this@MutableClock.instant()
            }

        override fun instant(): Instant = current
    }

    private companion object {
        const val SEOUL = "Asia/Seoul"

        /** UTC 로는 2026-05-25, 서울로는 2026-05-26 01:00. */
        val BASE_INSTANT: Instant = Instant.parse("2026-05-25T16:00:00Z")
        val TODAY: LocalDate = LocalDate.of(2026, 5, 26)
    }
}
