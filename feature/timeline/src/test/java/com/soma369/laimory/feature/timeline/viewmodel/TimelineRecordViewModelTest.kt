package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.navigation.TimelineEventEditorPage
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import com.soma369.laimory.core.domain.usecase.DeleteDailyRecordUseCase
import com.soma369.laimory.core.domain.usecase.GetDailyRecordUseCase
import com.soma369.laimory.core.domain.usecase.ObserveTimelineRecordUseCase
import com.soma369.laimory.core.domain.usecase.SaveTimelineRecordUseCase
import com.soma369.laimory.feature.timeline.state.TimelineDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiContent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiSideEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineRecordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTimelineRecordSessionRepository()
    private val recordRepository = RecordingTimelineRecordRepository()
    private val draftTaskCoordinator = FakeDraftTaskCoordinator()
    private val navigationHelper = RecordingNavigationHelper()

    @Test
    fun `단건 조회 성공은 기록을 세션에 저장하고 nullable 필드까지 화면에 전달한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            assertEquals(listOf(DAILY_RECORD_ID), recordRepository.requestedDailyRecordIds)
            assertEquals(DAILY_RECORD_ID, repository.timeline.value?.dailyRecordId)
            val content = viewModel.state.value.content as TimelineRecordUiContent.Record
            assertEquals(LocalDate.of(2026, 5, 8), content.value.recordDate)
            assertEquals(null, content.value.events.single().endAt)
            assertEquals(null, content.value.events.single().subtitle)
            assertEquals(null, content.value.events.single().memo)
        }

    @Test
    fun `이전 세션이 다른 기록을 가리켜도 선택한 기록을 조회해 표시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.save(timeline(events = emptyList()).copy(dailyRecordId = 99L))
            val viewModel = createLoadedViewModel()

            val content = viewModel.state.value.content as TimelineRecordUiContent.Record
            assertEquals(DAILY_RECORD_ID, content.value.dailyRecordId)
            assertEquals(DAILY_RECORD_ID, repository.timeline.value?.dailyRecordId)
        }

    @Test
    fun `Event가 없는 DailyTimeline은 접근 불가와 구분해 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel(record = timeline(events = emptyList()))

            val content = viewModel.state.value.content as TimelineRecordUiContent.Record
            assertTrue(content.value.events.isEmpty())
        }

    @Test
    fun `-404 단건 조회는 접근 불가 상태로 안내한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.dailyRecordResult =
                Result.failure(ApiException.ClientException(errorCode = -404, rawCode = 404))
            val viewModel = createViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.Initialize(DAILY_RECORD_ID))
            advanceUntilIdle()

            assertEquals(TimelineRecordUiContent.Unavailable, viewModel.state.value.content)
        }

    @Test
    fun `단건 조회 실패는 다시 시도 상태로 전환하고 재시도로 복구한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.dailyRecordResult = Result.failure(ApiException.NetworkException())
            val viewModel = createViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.Initialize(DAILY_RECORD_ID))
            advanceUntilIdle()

            assertEquals(TimelineRecordUiContent.LoadFailed, viewModel.state.value.content)

            recordRepository.dailyRecordResult = Result.success(timeline(events = listOf(event())))
            viewModel.sendIntent(TimelineRecordUiIntent.RetryLoad)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.content is TimelineRecordUiContent.Record)
        }

    @Test
    fun `같은 기록 재진입은 재조회 없이 표시를 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.Initialize(DAILY_RECORD_ID))
            advanceUntilIdle()

            assertEquals(listOf(DAILY_RECORD_ID), recordRepository.requestedDailyRecordIds)
            assertTrue(viewModel.state.value.content is TimelineRecordUiContent.Record)
        }

    @Test
    fun `표시 중이던 기록이 세션에서 사라지면 접근 불가 상태로 돌아간다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            repository.clear()
            runCurrent()

            assertEquals(TimelineRecordUiContent.Unavailable, viewModel.state.value.content)
        }

    @Test
    fun `뒤로가기 Intent는 공통 내비게이션 뒤로가기를 호출한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(TimelineRecordUiIntent.NavigateBack)
            runCurrent()

            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `Event 선택 Intent는 선택한 Event 편집 화면으로 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(TimelineRecordUiIntent.SelectEvent(timelineEventId = 17L))
            runCurrent()

            assertEquals(listOf(TimelineEventEditorPage(timelineEventId = 17L)), navigationHelper.pages)
        }

    @Test
    fun `삭제 요청 Intent는 하루 삭제 확인 상태를 연다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestDelete)
            runCurrent()

            assertEquals(TimelineDeleteDialogState.Confirmation, viewModel.state.value.deleteDialogState)
            assertEquals(DAILY_RECORD_ID, viewModel.state.value.deleteTarget?.dailyRecordId)
        }

    @Test
    fun `하루 삭제 성공은 기록과 홈 draft 상태를 초기화하고 완료 확인 뒤 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestDelete)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmDelete)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertEquals(listOf(DAILY_RECORD_ID), recordRepository.deletedDailyRecordIds)
            assertEquals(null, repository.timeline.value)
            assertEquals(1, draftTaskCoordinator.discardCount)
            assertEquals(DraftTaskTrackingState.Idle, draftTaskCoordinator.state.value)
            assertEquals(TimelineDeleteDialogState.Success, viewModel.state.value.deleteDialogState)
            assertEquals(0, navigationHelper.backCount)

            viewModel.sendIntent(TimelineRecordUiIntent.FinishDelete)
            runCurrent()

            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `사진 삭제 실패는 하루 기록과 홈 draft 상태를 유지하고 재시도할 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()
            recordRepository.failure = ApiException.ServerException(errorCode = -1017, rawCode = 502)

            viewModel.sendIntent(TimelineRecordUiIntent.RequestDelete)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.deleteDialogState is TimelineDeleteDialogState.RetryableError)
            assertTrue(repository.timeline.value != null)
            assertEquals(0, draftTaskCoordinator.discardCount)

            recordRepository.failure = null
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertEquals(TimelineDeleteDialogState.Success, viewModel.state.value.deleteDialogState)
            assertEquals(1, draftTaskCoordinator.discardCount)
        }

    @Test
    fun `삭제한 기록과 활성 draft 날짜가 다르면 draft 작업을 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            draftTaskCoordinator.setActiveTask(LocalDate.of(2026, 5, 7))
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestDelete)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertEquals(TimelineDeleteDialogState.Success, viewModel.state.value.deleteDialogState)
            assertEquals(0, draftTaskCoordinator.discardCount)
            val activeTask = (draftTaskCoordinator.state.value as DraftTaskTrackingState.WithTask).task
            assertEquals(LocalDate.of(2026, 5, 7), activeTask.recordDate)
        }

    @Test
    fun `이미 없는 하루 기록은 사용할 수 없는 상태로 전환하고 안내한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()
            recordRepository.failure =
                ApiException.ClientException(
                    errorCode = -404,
                    rawCode = 404,
                )

            viewModel.sendIntent(TimelineRecordUiIntent.RequestDelete)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertEquals(TimelineRecordUiContent.Unavailable, viewModel.state.value.content)
            assertEquals(TimelineDeleteDialogState.Hidden, viewModel.state.value.deleteDialogState)
            assertEquals(
                TimelineRecordUiSideEffect.ShowSnackbar("이미 삭제됐거나 접근할 수 없는 기록이에요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `작성 완료된 하루 기록 삭제는 다이얼로그를 닫고 안내한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()
            recordRepository.failure =
                ApiException.ConflictException(
                    errorCode = -1003,
                    rawCode = 409,
                )

            viewModel.sendIntent(TimelineRecordUiIntent.RequestDelete)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertEquals(TimelineDeleteDialogState.Hidden, viewModel.state.value.deleteDialogState)
            assertEquals(null, viewModel.state.value.deleteTarget)
            assertTrue(viewModel.state.value.content is TimelineRecordUiContent.Record)
            assertEquals(
                TimelineRecordUiSideEffect.ShowSnackbar("작성 완료된 기록은 삭제할 수 없어요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `공통 정책으로 처리된 실패는 추가 오류 다이얼로그 없이 닫는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()
            recordRepository.failure = ApiException.ServerException(rawCode = 500)

            viewModel.sendIntent(TimelineRecordUiIntent.RequestDelete)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertEquals(TimelineDeleteDialogState.Hidden, viewModel.state.value.deleteDialogState)
            assertEquals(null, viewModel.state.value.deleteTarget)
            assertTrue(repository.timeline.value != null)
            assertEquals(0, draftTaskCoordinator.discardCount)
        }

    private fun createViewModel() =
        TimelineRecordViewModel(
            observeTimelineRecordUseCase = ObserveTimelineRecordUseCase(repository),
            getDailyRecordUseCase =
                GetDailyRecordUseCase(
                    repository = recordRepository,
                    messageHelper = NoOpMessageHelper,
                ),
            saveTimelineRecordUseCase = SaveTimelineRecordUseCase(repository),
            deleteDailyRecordUseCase =
                DeleteDailyRecordUseCase(
                    repository = recordRepository,
                    sessionRepository = repository,
                    messageHelper = NoOpMessageHelper,
                ),
            draftTaskCoordinator = draftTaskCoordinator,
            navigationHelper = navigationHelper,
        )

    private fun TestScope.createLoadedViewModel(record: DailyTimeline = timeline(events = listOf(event()))): TimelineRecordViewModel {
        recordRepository.dailyRecordResult = Result.success(record)
        val viewModel = createViewModel()
        viewModel.sendIntent(TimelineRecordUiIntent.Initialize(DAILY_RECORD_ID))
        advanceUntilIdle()
        return viewModel
    }

    private fun timeline(events: List<TimelineEvent>) =
        DailyTimeline(
            dailyRecordId = DAILY_RECORD_ID,
            recordDate = LocalDate.of(2026, 5, 8),
            emotion = null,
            events = events,
        )

    private fun event() =
        TimelineEvent(
            timelineEventId = 1L,
            eventType = TimelineEventType.WORK,
            startAt = LocalDateTime.of(2026, 5, 8, 9, 0),
            endAt = null,
            title = "업무",
            subtitle = null,
            memo = null,
            items = emptyList(),
        )

    private class FakeTimelineRecordSessionRepository : TimelineRecordSessionRepository {
        private val mutableTimeline = MutableStateFlow<DailyTimeline?>(null)
        override val timeline: StateFlow<DailyTimeline?> = mutableTimeline

        override fun save(timeline: DailyTimeline) {
            mutableTimeline.value = timeline
        }

        override fun replaceEvent(event: TimelineEvent) = Unit

        override fun removeEvent(timelineEventId: Long) = Unit

        override fun clear() {
            mutableTimeline.value = null
        }
    }

    private class RecordingTimelineRecordRepository : TimelineRecordRepository {
        val requestedDailyRecordIds = mutableListOf<Long>()
        val deletedDailyRecordIds = mutableListOf<Long>()
        var dailyRecordResult: Result<DailyTimeline>? = null
        var failure: ApiException? = null

        override suspend fun getDailyRecords(): List<DailyTimeline> = error("사용하지 않음")

        override suspend fun getDailyRecord(dailyRecordId: Long): DailyTimeline {
            requestedDailyRecordIds += dailyRecordId
            val result = dailyRecordResult ?: error("사용하지 않음")
            return result.getOrThrow()
        }

        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent = error("사용하지 않음")

        override suspend fun deleteEvent(timelineEventId: Long) = error("사용하지 않음")

        override suspend fun deleteDailyRecord(dailyRecordId: Long) {
            failure?.let { throw it }
            deletedDailyRecordIds += dailyRecordId
        }
    }

    private class FakeDraftTaskCoordinator : DraftTaskCoordinator {
        private val mutableState =
            MutableStateFlow<DraftTaskTrackingState>(
                successState(LocalDate.of(2026, 5, 8)),
            )
        override val state: StateFlow<DraftTaskTrackingState> = mutableState
        var discardCount = 0

        fun setActiveTask(recordDate: LocalDate) {
            mutableState.value = successState(recordDate)
        }

        override suspend fun start(
            taskId: String,
            recordDate: LocalDate,
        ) = Unit

        override suspend fun onForeground() = Unit

        override suspend fun onBackground() = Unit

        override fun refreshFromCompletionSignal(taskId: String) = Unit

        override fun retry() = Unit

        override fun continueWaiting() = Unit

        override suspend fun discard() {
            discardCount++
            mutableState.value = DraftTaskTrackingState.Idle
        }

        private companion object {
            fun successState(recordDate: LocalDate) =
                DraftTaskTrackingState.Success(
                    task =
                        ActiveDraftTask(
                            taskId = "task-1",
                            recordDate = recordDate,
                            requestedAt = Instant.EPOCH,
                        ),
                    dailyRecordId = DAILY_RECORD_ID,
                )
        }
    }

    private data object NoOpMessageHelper : MessageHelper {
        override fun send(message: UserMessage) = Unit
    }

    private class RecordingNavigationHelper : NavigationHelper {
        var backCount = 0
        val pages = mutableListOf<Page>()

        override fun navigateTo(page: Page) {
            pages += page
        }

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() {
            backCount++
        }
    }

    private companion object {
        const val DAILY_RECORD_ID = 31L
    }
}
