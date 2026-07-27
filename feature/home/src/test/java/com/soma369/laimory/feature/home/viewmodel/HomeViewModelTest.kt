package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import com.soma369.laimory.core.domain.usecase.CreateTimelineDraftUseCase
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.DraftEndDay
import com.soma369.laimory.feature.home.state.HomeUiIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sourceRepository = FakeSourceItemRepository()
    private val draftRepository = FakeTimelineDraftRepository()
    private val draftTaskCoordinator = FakeDraftTaskCoordinator()
    private val navigationHelper = RecordingNavigationHelper()

    @Test
    fun `빈 범위에서는 초안 생성 요청을 보내지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()

            assertEquals(0, draftRepository.createCount)
            assertEquals(DraftCreationStatus.IDLE, viewModel.state.value.draftStatus)
        }

    @Test
    fun `생성 중 설정 변경과 중복 요청을 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            draftRepository.createGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.SelectEndDay(DraftEndDay.SAME_DAY))
            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()

            assertEquals(1, draftRepository.createCount)
            assertEquals(DraftCreationStatus.SUBMITTING, viewModel.state.value.draftStatus)
            assertEquals(DraftEndDay.NEXT_DAY, viewModel.state.value.endDay)

            draftRepository.createGate?.complete(DraftTaskHandle("task-1"))
            runCurrent()
            assertEquals(DraftCreationStatus.PROCESSING, viewModel.state.value.draftStatus)
        }

    @Test
    fun `생성 실패 후에는 재시도할 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            draftRepository.createFailure = IllegalStateException("failed")
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()
            assertEquals(DraftCreationStatus.FAILED, viewModel.state.value.draftStatus)

            draftRepository.createFailure = null
            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()

            assertEquals(2, draftRepository.createCount)
            assertEquals(DraftCreationStatus.PROCESSING, viewModel.state.value.draftStatus)
        }

    @Test
    fun `생성 완료 후 새 데이터만 수신해도 완료 상태를 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()
            sourceRepository.items.value = listOf(todayItem("first"), todayItem("second"))
            runCurrent()

            assertEquals(DraftCreationStatus.PROCESSING, viewModel.state.value.draftStatus)
        }

    @Test
    fun `SUCCESS 초안 보기는 작업을 유지하고 타임라인 화면으로 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()
            draftTaskCoordinator.emitSuccess(LocalDate.now(ZoneId.systemDefault()))
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.ViewDraft)
            runCurrent()

            assertEquals(0, draftTaskCoordinator.discardCount)
            assertEquals(listOf(TimelinePage), navigationHelper.destinations)
        }

    @Test
    fun `SUCCESS 카드 본문에서 날짜 선택을 요청하면 이동하지 않고 모달을 연다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()
            draftTaskCoordinator.emitSuccess(LocalDate.now(ZoneId.systemDefault()))
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ShowDatePicker)
            runCurrent()

            assertTrue(viewModel.state.value.isDatePickerVisible)
            assertTrue(navigationHelper.destinations.isEmpty())
        }

    @Test
    fun `SUCCESS 후 다른 날짜를 선택하면 해당 날짜의 새 초안 상태로 전환한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val draftDate = LocalDate.now(ZoneId.systemDefault())
            val otherDate = draftDate.minusDays(1)
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()
            draftTaskCoordinator.emitSuccess(draftDate)
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.SelectDate(otherDate))
            runCurrent()

            assertEquals(otherDate, viewModel.state.value.selectedDate)
            assertEquals(DraftCreationStatus.IDLE, viewModel.state.value.draftStatus)
            assertTrue(navigationHelper.destinations.isEmpty())
        }

    @Test
    fun `polling 오류에서 다시 시도하면 POST가 아니라 상태 조회를 재개한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()
            draftTaskCoordinator.emitRetryableError(LocalDate.now(ZoneId.systemDefault()))
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.RetryDraft)
            runCurrent()

            assertEquals(1, draftRepository.createCount)
            assertEquals(1, draftTaskCoordinator.retryCount)
        }

    @Test
    fun `과거 날짜 활성 작업이 있으면 홈 재진입 시 해당 날짜와 처리 상태를 복원한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val activeDate = LocalDate.now(ZoneId.systemDefault()).minusDays(12)
            draftTaskCoordinator.emitProcessing(activeDate)

            val viewModel = createViewModel()
            runCurrent()

            assertEquals(activeDate, viewModel.state.value.selectedDate)
            assertEquals(DraftCreationStatus.PROCESSING, viewModel.state.value.draftStatus)
        }

    @Test
    fun `사용자가 다른 날짜를 선택한 뒤 기존 작업이 재개돼도 선택 날짜를 되돌리지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val activeDate = LocalDate.now(ZoneId.systemDefault())
            val selectedDate = activeDate.minusDays(1)
            sourceRepository.items.value = listOf(todayItem("first"))
            draftTaskCoordinator.emitRetryableError(activeDate)
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.SelectDate(selectedDate))
            runCurrent()
            draftTaskCoordinator.emitProcessing(activeDate)
            runCurrent()

            assertEquals(selectedDate, viewModel.state.value.selectedDate)
            assertEquals(DraftCreationStatus.IDLE, viewModel.state.value.draftStatus)
        }

    private fun createViewModel(): HomeViewModel =
        HomeViewModel(
            observeSourceItemsUseCase = ObserveSourceItemsUseCase(sourceRepository),
            createTimelineDraftUseCase =
                CreateTimelineDraftUseCase(
                    repository = draftRepository,
                    messageHelper = NoOpMessageHelper,
                ),
            draftTaskCoordinator = draftTaskCoordinator,
            navigationHelper = navigationHelper,
        )

    private fun todayItem(id: String): SourceItem {
        val zone = ZoneId.systemDefault()
        val instant = LocalDate.now(zone).atTime(12, 0).atZone(zone).toInstant()
        return SourceItem(
            rawId = id,
            startAt = instant,
            endAt = null,
            timeZoneId = zone,
            payload = CalendarPayload("일정", null, null, false),
            sourceName = SourceName.CALENDAR_PROVIDER,
            sourceKey = id,
            collectedAt = instant,
        )
    }

    private class FakeSourceItemRepository : SourceItemRepository {
        val items = MutableStateFlow<List<SourceItem>>(emptyList())

        override suspend fun addAll(items: List<SourceItem>): Int = 0

        override suspend fun upsertAll(items: List<SourceItem>): Int = 0

        override fun observeAll(): Flow<List<SourceItem>> = items

        override suspend fun getLatestCollectedAt(itemType: ItemType): Instant? = null

        override suspend fun clear(itemType: ItemType) = Unit
    }

    private class FakeTimelineDraftRepository : TimelineDraftRepository {
        var createCount = 0
        var createGate: CompletableDeferred<DraftTaskHandle>? = null
        var createFailure: Throwable? = null

        override suspend fun uploadPhotos(clientPhotoUris: List<String>): List<String> = emptyList()

        override suspend fun createDraft(
            recordDate: LocalDate,
            zone: ZoneId,
            window: RecordDateWindow,
            items: List<SourceItem>,
            uploadedPhotoFilenames: Map<String, String>,
        ): DraftTaskHandle {
            createCount++
            createFailure?.let { throw it }
            return createGate?.await() ?: DraftTaskHandle("task-$createCount")
        }

        override suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot = throw UnsupportedOperationException()
    }

    private data object NoOpMessageHelper : MessageHelper {
        override fun send(message: UserMessage) = Unit
    }

    private class FakeDraftTaskCoordinator : DraftTaskCoordinator {
        private val mutableState = MutableStateFlow<DraftTaskTrackingState>(DraftTaskTrackingState.Idle)
        override val state: StateFlow<DraftTaskTrackingState> = mutableState
        var retryCount = 0
        var discardCount = 0

        override suspend fun start(
            taskId: String,
            recordDate: LocalDate,
        ) {
            mutableState.value =
                DraftTaskTrackingState.Processing(
                    ActiveDraftTask(taskId, recordDate, Instant.EPOCH),
                )
        }

        override suspend fun onForeground() = Unit

        override suspend fun onBackground() = Unit

        override fun refreshFromCompletionSignal(taskId: String) = Unit

        override fun retry() {
            retryCount++
        }

        override fun continueWaiting() = Unit

        override suspend fun discard() {
            discardCount++
            mutableState.value = DraftTaskTrackingState.Idle
        }

        fun emitSuccess(recordDate: LocalDate) {
            mutableState.value =
                DraftTaskTrackingState.Success(
                    ActiveDraftTask("task-1", recordDate, Instant.EPOCH),
                )
        }

        fun emitProcessing(recordDate: LocalDate) {
            mutableState.value =
                DraftTaskTrackingState.Processing(
                    ActiveDraftTask("task-1", recordDate, Instant.EPOCH),
                )
        }

        fun emitRetryableError(recordDate: LocalDate) {
            mutableState.value =
                DraftTaskTrackingState.RetryableError(
                    ActiveDraftTask("task-1", recordDate, Instant.EPOCH),
                )
        }
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
