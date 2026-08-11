package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import com.soma369.laimory.core.domain.usecase.CreateTimelineDraftUseCase
import com.soma369.laimory.feature.home.draft.DraftConsentSessionStore
import com.soma369.laimory.feature.home.state.DraftConsentTerm
import com.soma369.laimory.feature.home.state.DraftConsentTypeGroup
import com.soma369.laimory.feature.home.state.DraftConsentUiIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DraftConsentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone: ZoneId = ZoneId.systemDefault()
    private val date: LocalDate = LocalDate.now(zone)

    private val sessionStore = DraftConsentSessionStore()
    private val draftRepository = FakeTimelineDraftRepository()
    private val draftTaskCoordinator = FakeDraftTaskCoordinator()
    private val navigationHelper = RecordingNavigationHelper()

    @Test
    fun `새 스냅샷이 들어오면 내용을 구성하고 체크 상태를 초기화한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()
            prepare(listOf(calendarItem("cal")))
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.ToggleTerm(DraftConsentTerm.SENSITIVE_INFO))
            runCurrent()
            assertEquals(setOf(DraftConsentTerm.SENSITIVE_INFO), viewModel.state.value.checkedTerms)

            // 뒤로가기 후 재진입 = 같은 데이터라도 새 attemptId → 새 생성 시도로 초기화
            prepare(listOf(calendarItem("cal")))
            runCurrent()

            val state = viewModel.state.value
            assertNotNull(state.content)
            assertEquals(1, state.content!!.sentTotal)
            assertTrue(state.checkedTerms.isEmpty())
        }

    @Test
    fun `준비물이 없으면 내용 없이 홈 복귀 안내 상태를 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            assertNull(viewModel.state.value.content)
            assertFalse(viewModel.state.value.canSubmit)
        }

    @Test
    fun `필수 동의를 모두 완료하기 전에는 제출하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.ToggleTerm(DraftConsentTerm.SENSITIVE_INFO))
            viewModel.sendIntent(DraftConsentUiIntent.ToggleTerm(DraftConsentTerm.THIRD_PARTY_PROVISION))
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertFalse(viewModel.state.value.canSubmit)
            assertEquals(0, draftRepository.uploadCount)
            assertEquals(0, draftRepository.createCount)
            assertFalse(viewModel.state.value.isSubmitting)
        }

    @Test
    fun `모든 동의 후 제출하면 스냅샷 그대로 전송하고 홈으로 복귀한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal"), photoItem(7L)))
            val snapshotItems = sessionStore.preparation.value!!.selection.items
            val viewModel = createViewModel()
            runCurrent()

            checkAllTerms(viewModel)
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertEquals(1, draftRepository.createCount)
            assertEquals(snapshotItems, draftRepository.createdItems)
            assertEquals(listOf("content://photo/7"), draftRepository.uploadedUris)
            assertEquals(listOf("task-1"), draftTaskCoordinator.startedTaskIds)
            assertNull(sessionStore.preparation.value)
            assertTrue(sessionStore.consumeSubmittedResult())
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `제출 중 중복 요청과 체크 변경을 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal")))
            draftRepository.createGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()

            checkAllTerms(viewModel)
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            viewModel.sendIntent(DraftConsentUiIntent.ToggleTerm(DraftConsentTerm.SENSITIVE_INFO))
            runCurrent()

            assertEquals(1, draftRepository.createCount)
            assertTrue(viewModel.state.value.isSubmitting)
            assertTrue(viewModel.state.value.isAllTermsChecked)

            draftRepository.createGate?.complete(DraftTaskHandle("task-1"))
            runCurrent()
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `제출 실패 시 동의 화면에 머물러 같은 스냅샷으로 재시도할 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal")))
            draftRepository.createFailure = IllegalStateException("failed")
            val viewModel = createViewModel()
            runCurrent()

            checkAllTerms(viewModel)
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertNotNull(viewModel.state.value.submitError)
            assertFalse(viewModel.state.value.isSubmitting)
            assertEquals(0, navigationHelper.backCount)
            assertNotNull(sessionStore.preparation.value)
            assertFalse(sessionStore.consumeSubmittedResult())

            draftRepository.createFailure = null
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertEquals(2, draftRepository.createCount)
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `뒤로가기는 준비 상태를 폐기하고 네트워크를 호출하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal"), photoItem(7L)))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.NavigateBack)
            runCurrent()

            assertNull(sessionStore.preparation.value)
            assertEquals(1, navigationHelper.backCount)
            assertEquals(0, draftRepository.uploadCount)
            assertEquals(0, draftRepository.createCount)
        }

    @Test
    fun `제출 중에는 뒤로가기를 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal")))
            draftRepository.createGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()

            checkAllTerms(viewModel)
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.NavigateBack)
            runCurrent()

            assertEquals(0, navigationHelper.backCount)
            assertNotNull(sessionStore.preparation.value)

            draftRepository.createGate?.complete(DraftTaskHandle("task-1"))
            runCurrent()
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `이전 작업 폐기 플래그가 있으면 제출 직전에 폐기한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal")), discardActiveTask = true)
            val viewModel = createViewModel()
            runCurrent()

            checkAllTerms(viewModel)
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertEquals(1, draftTaskCoordinator.discardCount)
            assertEquals(listOf("task-1"), draftTaskCoordinator.startedTaskIds)
        }

    @Test
    fun `전송 0건 유형의 상세는 열리지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.OpenTypeDetail(DraftConsentTypeGroup.PHOTO))
            runCurrent()
            assertNull(viewModel.state.value.openTypeDetail)

            viewModel.sendIntent(DraftConsentUiIntent.OpenTypeDetail(DraftConsentTypeGroup.CALENDAR))
            runCurrent()
            assertEquals(DraftConsentTypeGroup.CALENDAR, viewModel.state.value.openTypeDetail)
        }

    private fun createViewModel(): DraftConsentViewModel =
        DraftConsentViewModel(
            sessionStore = sessionStore,
            createTimelineDraftUseCase =
                CreateTimelineDraftUseCase(
                    repository = draftRepository,
                    messageHelper = NoOpMessageHelper,
                ),
            draftTaskCoordinator = draftTaskCoordinator,
            navigationHelper = navigationHelper,
        )

    private fun checkAllTerms(viewModel: DraftConsentViewModel) {
        DraftConsentTerm.entries.forEach { term ->
            viewModel.sendIntent(DraftConsentUiIntent.ToggleTerm(term))
        }
    }

    private fun prepare(
        items: List<SourceItem>,
        discardActiveTask: Boolean = false,
    ) {
        val window = RecordDateWindow.ofDate(date, zone)
        val selection = DraftSourceItemSelectionPolicy().select(window, items).getOrThrow()
        sessionStore.prepare(
            recordDate = date,
            zone = zone,
            window = window,
            selection = selection,
            discardActiveTask = discardActiveTask,
        )
    }

    private fun calendarItem(id: String): SourceItem {
        val instant = date.atTime(12, 0).atZone(zone).toInstant()
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

    private fun photoItem(id: Long): SourceItem {
        val instant = date.atTime(13, 0).atZone(zone).toInstant()
        return SourceItem(
            rawId = "photo-$id",
            startAt = instant,
            endAt = null,
            timeZoneId = zone,
            payload = PhotoPayload("$id.jpg", "content://photo/$id", null, null, null),
            sourceName = SourceName.MEDIA_STORE,
            sourceKey = id.toString(),
            collectedAt = instant,
        )
    }

    private data object NoOpMessageHelper : MessageHelper {
        override fun send(message: UserMessage) = Unit
    }

    private class FakeTimelineDraftRepository : TimelineDraftRepository {
        var uploadCount = 0
        var createCount = 0
        var createGate: CompletableDeferred<DraftTaskHandle>? = null
        var createFailure: Throwable? = null
        var createdItems: List<SourceItem> = emptyList()
        var uploadedUris: List<String>? = null

        override suspend fun uploadPhotos(clientPhotoUris: List<String>): List<String> {
            uploadCount++
            uploadedUris = clientPhotoUris
            return clientPhotoUris.mapIndexed { index, _ -> "uploaded-$index.jpg" }
        }

        override suspend fun createDraft(
            recordDate: LocalDate,
            zone: ZoneId,
            window: RecordDateWindow,
            items: List<SourceItem>,
            uploadedPhotoFilenames: Map<String, String>,
        ): DraftTaskHandle {
            createCount++
            createdItems = items
            createFailure?.let { throw it }
            return createGate?.await() ?: DraftTaskHandle("task-$createCount")
        }

        override suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot = throw UnsupportedOperationException()
    }

    private class FakeDraftTaskCoordinator : DraftTaskCoordinator {
        private val mutableState = MutableStateFlow<DraftTaskTrackingState>(DraftTaskTrackingState.Idle)
        override val state: StateFlow<DraftTaskTrackingState> = mutableState
        val startedTaskIds = mutableListOf<String>()
        var discardCount = 0

        override suspend fun start(
            taskId: String,
            recordDate: LocalDate,
        ) {
            startedTaskIds += taskId
            mutableState.value =
                DraftTaskTrackingState.Processing(
                    ActiveDraftTask(taskId, recordDate, Instant.EPOCH),
                )
        }

        override suspend fun onForeground() = Unit

        override suspend fun onBackground() = Unit

        override fun refreshFromCompletionSignal(taskId: String) = Unit

        override fun retry() = Unit

        override fun continueWaiting() = Unit

        override suspend fun discard() {
            discardCount++
            mutableState.value = DraftTaskTrackingState.Idle
        }
    }

    private class RecordingNavigationHelper : NavigationHelper {
        val destinations = mutableListOf<Page>()
        var backCount = 0

        override fun navigateTo(page: Page) {
            destinations += page
        }

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() {
            backCount++
        }
    }
}
