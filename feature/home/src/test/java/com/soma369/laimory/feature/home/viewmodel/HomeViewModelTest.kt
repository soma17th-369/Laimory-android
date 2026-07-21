package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.navigation.Page
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
            assertEquals(DraftCreationStatus.SUBMITTED, viewModel.state.value.draftStatus)
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
            assertEquals(DraftCreationStatus.SUBMITTED, viewModel.state.value.draftStatus)
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

            assertEquals(DraftCreationStatus.SUBMITTED, viewModel.state.value.draftStatus)
        }

    @Test
    fun `생성 완료 후 범위를 변경하면 다시 생성할 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.SelectDate(LocalDate.now().minusDays(1)))
            runCurrent()

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
            navigationHelper = NoOpNavigationHelper,
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

    private data object NoOpNavigationHelper : NavigationHelper {
        override fun navigateTo(page: Page) = Unit

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() = Unit
    }
}
