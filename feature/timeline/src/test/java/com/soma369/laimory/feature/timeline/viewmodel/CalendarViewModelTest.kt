package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.usecase.GetMonthlyDailyRecordsUseCase
import com.soma369.laimory.core.ui.theme.Emotion
import com.soma369.laimory.feature.timeline.model.CALENDAR_FIRST_MONTH
import com.soma369.laimory.feature.timeline.model.CALENDAR_LAST_MONTH
import com.soma369.laimory.feature.timeline.state.CalendarUiIntent
import com.soma369.laimory.feature.timeline.state.MonthlyRecordsUiContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

    private val repository = RecordingMonthlyRepository()
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
            assertEquals(THIS_MONTH, viewModel.state.value.visibleMonth)
        }

    @Test
    fun `동기화는 표시 월과 이웃 월을 함께 조회하고 각 월 슬롯에 담는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.recordsByMonth =
                mapOf(THIS_MONTH to listOf(MonthlyDailyRecord(TODAY, TimelineEmotion.VERY_HAPPY)))
            val viewModel = createViewModel()

            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(
                setOf(THIS_MONTH, THIS_MONTH.minusMonths(1), THIS_MONTH.plusMonths(1)),
                repository.requestedMonths.toSet(),
            )
            assertEquals(Emotion.JOY, viewModel.state.value.recordOf(TODAY)?.emotion)
            // 기록이 없는 이웃 월도 빈 격자를 가진 정상 상태다.
            assertEquals(
                MonthlyRecordsUiContent.Records(emptyMap()),
                viewModel.state.value.contentOf(THIS_MONTH.plusMonths(1)),
            )
        }

    @Test
    fun `조회 중인 월만 로딩이고 다른 월은 병렬로 채워진다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.gates[THIS_MONTH] = CompletableDeferred()
            val viewModel = createViewModel()

            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(MonthlyRecordsUiContent.Loading, viewModel.state.value.contentOf(THIS_MONTH))
            assertTrue(viewModel.state.value.contentOf(THIS_MONTH.minusMonths(1)) is MonthlyRecordsUiContent.Records)
            assertTrue(viewModel.state.value.contentOf(THIS_MONTH.plusMonths(1)) is MonthlyRecordsUiContent.Records)
        }

    @Test
    fun `같은 월 요청은 중복 차단하고 캐시된 월은 다시 부르지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.gates[THIS_MONTH] = CompletableDeferred()
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            // 진행 중인 월도, 이미 받아 둔 이웃 월도 다시 요청하지 않는다.
            viewModel.sendIntent(CalendarUiIntent.ShowMonth(THIS_MONTH))
            advanceUntilIdle()

            assertEquals(1, repository.requestedMonths.count { it == THIS_MONTH })
            assertEquals(1, repository.requestedMonths.count { it == THIS_MONTH.minusMonths(1) })
        }

    @Test
    fun `왕복 스와이프는 캐시를 재사용하고 새로 보이는 달만 조회한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()
            repository.requestedMonths.clear()

            viewModel.sendIntent(CalendarUiIntent.ShowMonth(THIS_MONTH.plusMonths(1)))
            advanceUntilIdle()
            viewModel.sendIntent(CalendarUiIntent.ShowMonth(THIS_MONTH))
            advanceUntilIdle()

            // 6월로 넘어가며 7월만 새로 받고, 5월로 돌아올 때는 아무것도 받지 않는다.
            assertEquals(listOf(THIS_MONTH.plusMonths(2)), repository.requestedMonths)
        }

    @Test
    fun `복귀 재동기화는 격자를 비우지 않고 표시 월을 다시 검증한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.recordsByMonth =
                mapOf(THIS_MONTH to listOf(MonthlyDailyRecord(TODAY, TimelineEmotion.HAPPY)))
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            val revalidationGate = CompletableDeferred<List<MonthlyDailyRecord>>()
            repository.gates[THIS_MONTH] = revalidationGate
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            // 재검증이 끝나기 전에도 보고 있던 격자는 그대로 남는다. (깜빡임 방지)
            val revalidating = viewModel.state.value.contentOf(THIS_MONTH) as MonthlyRecordsUiContent.Records
            assertEquals(setOf(TODAY), revalidating.recordsByDate.keys)
            assertTrue(revalidating.isStale)

            revalidationGate.complete(listOf(MonthlyDailyRecord(TODAY.minusDays(1), TimelineEmotion.VERY_UNHAPPY)))
            advanceUntilIdle()

            val revalidated = viewModel.state.value.contentOf(THIS_MONTH) as MonthlyRecordsUiContent.Records
            assertEquals(setOf(TODAY.minusDays(1)), revalidated.recordsByDate.keys)
            assertTrue(!revalidated.isStale)
        }

    @Test
    fun `캐시가 있는 월의 재조회 실패는 기존 내용을 유지하고 안내한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.recordsByMonth =
                mapOf(THIS_MONTH to listOf(MonthlyDailyRecord(TODAY, TimelineEmotion.HAPPY)))
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            val messages = collectSnackbars(viewModel)

            repository.failure = ApiException.NetworkException()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            val content = viewModel.state.value.contentOf(THIS_MONTH) as MonthlyRecordsUiContent.Records
            assertEquals(setOf(TODAY), content.recordsByDate.keys)
            assertEquals(listOf(ApiException.NETWORK_ERROR), messages)
        }

    @Test
    fun `내용이 없는 월의 실패는 그 월 페이지에 남고 다시 시도로 복구한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.failure = ApiException.NetworkException()
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(MonthlyRecordsUiContent.LoadFailed, viewModel.state.value.contentOf(THIS_MONTH))

            repository.failure = null
            repository.recordsByMonth =
                mapOf(THIS_MONTH to listOf(MonthlyDailyRecord(TODAY, TimelineEmotion.NEUTRAL)))
            viewModel.sendIntent(CalendarUiIntent.RetryMonth(THIS_MONTH))
            advanceUntilIdle()

            assertEquals(Emotion.MELLOW, viewModel.state.value.recordOf(TODAY)?.emotion)
        }

    @Test
    fun `이웃 월 prefetch 실패는 안내하지 않고 그 월에만 남는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.failure = ApiException.NetworkException()
            repository.failingMonths = setOf(THIS_MONTH.plusMonths(1))
            val viewModel = createViewModel()
            val messages = collectSnackbars(viewModel)

            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            // 사용자가 하지 않은 조작으로 스낵바가 뜨지 않아야 한다.
            assertTrue(messages.isEmpty())
            assertEquals(MonthlyRecordsUiContent.LoadFailed, viewModel.state.value.contentOf(THIS_MONTH.plusMonths(1)))
            assertTrue(viewModel.state.value.contentOf(THIS_MONTH) is MonthlyRecordsUiContent.Records)
        }

    @Test
    fun `prefetch 하던 달로 넘어온 뒤 실패하면 그 실패를 안내한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val nextMonth = THIS_MONTH.plusMonths(1)
            val gate = CompletableDeferred<List<MonthlyDailyRecord>>()
            repository.gates[nextMonth] = gate
            val viewModel = createViewModel()
            val messages = collectSnackbars(viewModel)
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            // prefetch 로 시작한 요청이지만 도착 전에 사용자가 그 달로 넘어와 있다.
            viewModel.sendIntent(CalendarUiIntent.ShowMonth(nextMonth))
            advanceUntilIdle()
            gate.completeExceptionally(ApiException.NetworkException())
            advanceUntilIdle()

            assertEquals(listOf(ApiException.NETWORK_ERROR), messages)
        }

    @Test
    fun `이미 떠난 달의 실패는 뒤늦게 도착해도 안내하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<List<MonthlyDailyRecord>>()
            repository.gates[THIS_MONTH] = gate
            val viewModel = createViewModel()
            val messages = collectSnackbars(viewModel)
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            viewModel.sendIntent(CalendarUiIntent.ShowMonth(THIS_MONTH.plusMonths(2)))
            advanceUntilIdle()
            gate.completeExceptionally(ApiException.NetworkException())
            advanceUntilIdle()

            assertTrue(messages.isEmpty())
            assertEquals(MonthlyRecordsUiContent.LoadFailed, viewModel.state.value.contentOf(THIS_MONTH))
        }

    @Test
    fun `무효화 이전에 시작된 응답은 새 상태를 되살리지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val staleGate = CompletableDeferred<List<MonthlyDailyRecord>>()
            repository.gates[THIS_MONTH] = staleGate
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            repository.recordsByMonth =
                mapOf(THIS_MONTH to listOf(MonthlyDailyRecord(TODAY, TimelineEmotion.HAPPY)))
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()
            assertEquals(Emotion.CALM, viewModel.state.value.recordOf(TODAY)?.emotion)

            // 무효화 이전 요청이 뒤늦게 도착해도 이미 반영된 최신 결과를 덮지 않는다.
            staleGate.complete(listOf(MonthlyDailyRecord(TODAY, TimelineEmotion.VERY_UNHAPPY)))
            advanceUntilIdle()

            assertEquals(Emotion.CALM, viewModel.state.value.recordOf(TODAY)?.emotion)
        }

    @Test
    fun `늦게 온 이웃 월 응답은 그 월 슬롯에만 기록된다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val neighborGate = CompletableDeferred<List<MonthlyDailyRecord>>()
            repository.gates[THIS_MONTH.minusMonths(1)] = neighborGate
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            viewModel.sendIntent(CalendarUiIntent.ShowMonth(THIS_MONTH.plusMonths(1)))
            advanceUntilIdle()

            val neighborDate = TODAY.minusMonths(1)
            neighborGate.complete(listOf(MonthlyDailyRecord(neighborDate, TimelineEmotion.HAPPY)))
            advanceUntilIdle()

            assertEquals(THIS_MONTH.plusMonths(1), viewModel.state.value.visibleMonth)
            assertEquals(Emotion.CALM, viewModel.state.value.recordOf(neighborDate)?.emotion)
            assertTrue(viewModel.state.value.recordsOf(THIS_MONTH.plusMonths(1)).isEmpty())
        }

    @Test
    fun `조회 중인 월의 날짜 탭은 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.gates[THIS_MONTH] = CompletableDeferred()
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            viewModel.sendIntent(CalendarUiIntent.SelectDate(TODAY.minusDays(2)))
            advanceUntilIdle()

            // 기록 유무를 모르는 동안에는 선택도 이동도 하지 않는다.
            assertTrue(navigationHelper.pages.isEmpty())
            assertEquals(TODAY, viewModel.state.value.selectedDate)
        }

    @Test
    fun `기록이 있는 날짜를 선택하면 단건 조회 화면으로 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.recordsByMonth =
                mapOf(THIS_MONTH to listOf(MonthlyDailyRecord(TODAY, TimelineEmotion.HAPPY)))
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
            repository.recordsByMonth =
                mapOf(THIS_MONTH to listOf(MonthlyDailyRecord(TODAY, TimelineEmotion.HAPPY)))
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
            repository.recordsByMonth =
                mapOf(THIS_MONTH to listOf(MonthlyDailyRecord(TODAY, TimelineEmotion.HAPPY)))
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            repository.recordsByMonth = emptyMap()
            viewModel.sendIntent(CalendarUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(TODAY, viewModel.state.value.selectedDate)
            assertNull(viewModel.state.value.recordOf(TODAY))

            viewModel.sendIntent(CalendarUiIntent.SelectDate(TODAY))
            advanceUntilIdle()
            assertTrue(navigationHelper.pages.isEmpty())
        }

    @Test
    fun `월 전환은 서버 허용 범위 밖으로 나가지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(CalendarUiIntent.ShowMonth(CALENDAR_FIRST_MONTH.minusMonths(1)))
            advanceUntilIdle()
            assertEquals(CALENDAR_FIRST_MONTH, viewModel.state.value.visibleMonth)

            viewModel.sendIntent(CalendarUiIntent.ShowMonth(CALENDAR_LAST_MONTH.plusMonths(1)))
            advanceUntilIdle()
            assertEquals(CALENDAR_LAST_MONTH, viewModel.state.value.visibleMonth)

            assertEquals(TODAY, viewModel.state.value.selectedDate)
        }

    @Test
    fun `범위 경계에서는 바깥 이웃 월을 조회하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(CalendarUiIntent.ShowMonth(CALENDAR_FIRST_MONTH))
            advanceUntilIdle()

            assertTrue(repository.requestedMonths.contains(CALENDAR_FIRST_MONTH))
            assertTrue(repository.requestedMonths.contains(CALENDAR_FIRST_MONTH.plusMonths(1)))
            assertTrue(repository.requestedMonths.none { it.year < CALENDAR_FIRST_MONTH.year })
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
            assertEquals(THIS_MONTH, viewModel.state.value.visibleMonth)
            assertEquals(TODAY, viewModel.state.value.selectedDate)
        }

    @Test
    fun `연·월 피커는 표시 중인 월의 연도로 열린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.ShowMonth(YearMonth.of(2025, 12)))
            advanceUntilIdle()

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
            assertEquals(THIS_MONTH, viewModel.state.value.visibleMonth)
        }

    @Test
    fun `피커 연도는 서버 허용 범위에서 멈춘다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.sendIntent(CalendarUiIntent.ShowMonth(CALENDAR_FIRST_MONTH))
            viewModel.sendIntent(CalendarUiIntent.OpenMonthPicker)
            advanceUntilIdle()

            viewModel.sendIntent(CalendarUiIntent.ShowPreviousPickerYear)
            advanceUntilIdle()

            assertEquals(CALENDAR_FIRST_MONTH.year, viewModel.state.value.monthPicker?.year)
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
            // 고른 달도 이웃과 함께 채운다.
            assertTrue(repository.requestedMonths.contains(YearMonth.of(2025, 11)))
            assertTrue(repository.requestedMonths.contains(YearMonth.of(2025, 12)))
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
            assertEquals(THIS_MONTH, viewModel.state.value.visibleMonth)

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

    /**
     * 스낵바를 즉시 수집한다.
     *
     * 기본 디스패처로 collect 를 걸면 발행 시점에 수집이 아직 시작되지 않아 메시지를 놓친다 —
     * 수집 자체를 검증에 쓰려면 unconfined 로 붙여야 한다.
     */
    private fun TestScope.collectSnackbars(viewModel: CalendarViewModel): List<String> {
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.snackbar.toList(messages) }
        return messages
    }

    private fun createViewModel() =
        CalendarViewModel(
            getMonthlyDailyRecordsUseCase = GetMonthlyDailyRecordsUseCase(repository, messageHelper),
            navigationHelper = navigationHelper,
            clock = clock,
        )

    private class RecordingMonthlyRepository : TimelineRecordRepository {
        val requestedMonths = mutableListOf<YearMonth>()
        val gates = mutableMapOf<YearMonth, CompletableDeferred<List<MonthlyDailyRecord>>>()
        var recordsByMonth: Map<YearMonth, List<MonthlyDailyRecord>> = emptyMap()
        var failure: ApiException? = null

        /** 비어 있으면 모든 월이 실패한다. */
        var failingMonths: Set<YearMonth> = emptySet()

        override suspend fun getMonthlyDailyRecords(month: YearMonth): List<MonthlyDailyRecord> {
            requestedMonths += month
            gates.remove(month)?.let { pending -> return pending.await() }
            failure?.takeIf { failingMonths.isEmpty() || month in failingMonths }?.let { throw it }
            return recordsByMonth[month].orEmpty()
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
        val THIS_MONTH: YearMonth = YearMonth.of(2026, 5)
    }
}
