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
import com.soma369.laimory.core.domain.usecase.UpdateTimelineEventMemoUseCase
import com.soma369.laimory.feature.timeline.state.TimelineDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiContent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiSideEffect
import kotlinx.coroutines.CompletableDeferred
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

            assertEquals(listOf(RECORD_DATE), recordRepository.requestedRecordDates)
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
            repository.save(
                timeline(events = emptyList()).copy(
                    dailyRecordId = 99L,
                    recordDate = OTHER_RECORD_DATE,
                ),
            )
            val viewModel = createLoadedViewModel()

            val content = viewModel.state.value.content as TimelineRecordUiContent.Record
            assertEquals(DAILY_RECORD_ID, content.value.dailyRecordId)
            assertEquals(DAILY_RECORD_ID, repository.timeline.value?.dailyRecordId)
        }

    @Test
    fun `세션에 같은 기록이 선저장돼 있어도 조회 성공을 화면에 반영한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val record = timeline(events = listOf(event()))
            repository.save(record)

            val viewModel = createLoadedViewModel(record = record)

            val content = viewModel.state.value.content as TimelineRecordUiContent.Record
            assertEquals(DAILY_RECORD_ID, content.value.dailyRecordId)
        }

    @Test
    fun `조회 중 다른 기록을 요청하면 새 기록이 우선하고 이전 응답은 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<DailyTimeline>()
            recordRepository.dailyRecordGate = gate
            recordRepository.dailyRecordResult =
                Result.success(
                    timeline(events = emptyList()).copy(
                        dailyRecordId = 32L,
                        recordDate = OTHER_RECORD_DATE,
                    ),
                )
            val viewModel = createViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.Initialize(RECORD_DATE))
            runCurrent()
            viewModel.sendIntent(TimelineRecordUiIntent.Initialize(OTHER_RECORD_DATE))
            advanceUntilIdle()

            assertEquals(listOf(RECORD_DATE, OTHER_RECORD_DATE), recordRepository.requestedRecordDates)
            val content = viewModel.state.value.content as TimelineRecordUiContent.Record
            assertEquals(32L, content.value.dailyRecordId)

            gate.complete(timeline(events = listOf(event())))
            advanceUntilIdle()

            val finalContent = viewModel.state.value.content as TimelineRecordUiContent.Record
            assertEquals(32L, finalContent.value.dailyRecordId)
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

            viewModel.sendIntent(TimelineRecordUiIntent.Initialize(RECORD_DATE))
            advanceUntilIdle()

            assertEquals(TimelineRecordUiContent.Unavailable, viewModel.state.value.content)
        }

    @Test
    fun `단건 조회 실패는 다시 시도 상태로 전환하고 재시도로 복구한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.dailyRecordResult = Result.failure(ApiException.NetworkException())
            val viewModel = createViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.Initialize(RECORD_DATE))
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

            viewModel.sendIntent(TimelineRecordUiIntent.Initialize(RECORD_DATE))
            advanceUntilIdle()

            assertEquals(listOf(RECORD_DATE), recordRepository.requestedRecordDates)
            assertTrue(viewModel.state.value.content is TimelineRecordUiContent.Record)
        }

    @Test
    fun `날짜 인자가 없거나 잘못되면 서버를 호출하지 않고 접근 불가 상태로 전환한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.Initialize(null))
            advanceUntilIdle()

            assertEquals(TimelineRecordUiContent.Unavailable, viewModel.state.value.content)
            assertTrue(recordRepository.requestedRecordDates.isEmpty())
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
    fun `편집하지 않을 때 뒤로가기는 공통 내비게이션 뒤로가기를 호출한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(TimelineRecordUiIntent.NavigateBack)
            runCurrent()

            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `메모 편집 중 뒤로가기는 화면을 유지하고 편집기만 닫는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()
            viewModel.sendIntent(TimelineRecordUiIntent.EditMemo(timelineEventId = 1L))
            runCurrent()

            viewModel.sendIntent(TimelineRecordUiIntent.NavigateBack)
            runCurrent()

            assertEquals(null, viewModel.state.value.memoEditor)
            assertEquals(0, navigationHelper.backCount)
        }

    @Test
    fun `메모 저장 중 뒤로가기는 저장과 화면을 그대로 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            recordRepository.memoUpdateGate = gate
            val viewModel = createLoadedViewModel()
            viewModel.sendIntent(TimelineRecordUiIntent.EditMemo(timelineEventId = 1L))
            viewModel.sendIntent(TimelineRecordUiIntent.ChangeMemo("저장 중인 메모"))
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmMemoEdit)
            runCurrent()

            viewModel.sendIntent(TimelineRecordUiIntent.NavigateBack)
            runCurrent()

            assertEquals(true, viewModel.state.value.memoEditor?.isSaving)
            assertEquals(0, navigationHelper.backCount)

            gate.complete(Unit)
            advanceUntilIdle()
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
    fun `메모 영역을 선택하면 기존 값으로 인라인 편집하고 취소하면 원래 표시로 돌아간다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel =
                createLoadedViewModel(
                    record = timeline(events = listOf(event(memo = "기존 메모"))),
                )

            viewModel.sendIntent(TimelineRecordUiIntent.EditMemo(timelineEventId = 1L))
            viewModel.sendIntent(TimelineRecordUiIntent.ChangeMemo("수정 중인 메모"))
            runCurrent()

            assertEquals("기존 메모", viewModel.state.value.memoEditor?.originalMemo)
            assertEquals("수정 중인 메모", viewModel.state.value.memoEditor?.draftMemo)

            viewModel.sendIntent(TimelineRecordUiIntent.CancelMemoEdit)
            runCurrent()

            assertEquals(null, viewModel.state.value.memoEditor)
            assertTrue(recordRepository.updatedMemos.isEmpty())
        }

    @Test
    fun `메모 완료는 전용 PUT 결과를 세션과 카드에 반영한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.EditMemo(timelineEventId = 1L))
            viewModel.sendIntent(TimelineRecordUiIntent.ChangeMemo("오늘의 메모"))
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmMemoEdit)
            advanceUntilIdle()

            assertEquals(listOf(1L to "오늘의 메모"), recordRepository.updatedMemos)
            assertEquals("오늘의 메모", repository.timeline.value?.events?.single()?.memo)
            val content = viewModel.state.value.content as TimelineRecordUiContent.Record
            assertEquals("오늘의 메모", content.value.events.single().memo)
            assertEquals(null, viewModel.state.value.memoEditor)
        }

    @Test
    fun `공백뿐인 메모 완료는 null 제거 요청을 보낸다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel =
                createLoadedViewModel(
                    record = timeline(events = listOf(event(memo = "지울 메모"))),
                )

            viewModel.sendIntent(TimelineRecordUiIntent.EditMemo(timelineEventId = 1L))
            viewModel.sendIntent(TimelineRecordUiIntent.ChangeMemo("   "))
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmMemoEdit)
            advanceUntilIdle()

            assertEquals(listOf(1L to null), recordRepository.updatedMemos)
            assertEquals(null, repository.timeline.value?.events?.single()?.memo)
        }

    @Test
    fun `메모 최대 길이를 초과하면 완료 요청을 보내지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.EditMemo(timelineEventId = 1L))
            viewModel.sendIntent(TimelineRecordUiIntent.ChangeMemo("가".repeat(10_001)))
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmMemoEdit)
            advanceUntilIdle()

            assertEquals(false, viewModel.state.value.memoEditor?.isValid)
            assertTrue(recordRepository.updatedMemos.isEmpty())
        }

    @Test
    fun `메모 최대 길이까지는 완료 요청을 보낸다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val maxLengthMemo = "가".repeat(10_000)
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.EditMemo(timelineEventId = 1L))
            viewModel.sendIntent(TimelineRecordUiIntent.ChangeMemo(maxLengthMemo))
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmMemoEdit)
            advanceUntilIdle()

            assertEquals(listOf(1L to maxLengthMemo), recordRepository.updatedMemos)
            assertEquals(null, viewModel.state.value.memoEditor)
        }

    @Test
    fun `메모 수정 네트워크 실패는 입력값을 유지해 다시 시도할 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.failure = ApiException.NetworkException()
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.EditMemo(timelineEventId = 1L))
            viewModel.sendIntent(TimelineRecordUiIntent.ChangeMemo("연결되면 다시 저장할 메모"))
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmMemoEdit)
            advanceUntilIdle()

            assertEquals("연결되면 다시 저장할 메모", viewModel.state.value.memoEditor?.draftMemo)
            assertEquals(false, viewModel.state.value.memoEditor?.isSaving)
        }

    @Test
    fun `메모 저장 중 중복 완료 요청을 막는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            recordRepository.memoUpdateGate = gate
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.EditMemo(timelineEventId = 1L))
            viewModel.sendIntent(TimelineRecordUiIntent.ChangeMemo("한 번만 저장"))
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmMemoEdit)
            runCurrent()
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmMemoEdit)
            runCurrent()

            assertEquals(listOf(1L to "한 번만 저장"), recordRepository.updatedMemos)
            assertEquals(true, viewModel.state.value.memoEditor?.isSaving)

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf(1L to "한 번만 저장"), recordRepository.updatedMemos)
            assertEquals(null, viewModel.state.value.memoEditor)
        }

    @Test
    fun `작성 완료된 기록의 메모 수정은 편집을 닫고 안내한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.failure =
                ApiException.ConflictException(
                    errorCode = -1003,
                    rawCode = 409,
                )
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.EditMemo(timelineEventId = 1L))
            viewModel.sendIntent(TimelineRecordUiIntent.ChangeMemo("수정할 메모"))
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmMemoEdit)
            advanceUntilIdle()

            assertEquals(null, viewModel.state.value.memoEditor)
            assertEquals(
                TimelineRecordUiSideEffect.ShowSnackbar("작성 완료된 기록은 수정할 수 없어요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `삭제 요청 Intent는 하루 삭제 확인 상태를 연다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestDelete)
            runCurrent()

            assertEquals(TimelineDeleteDialogState.Confirmation, viewModel.state.value.deleteDialogState)
            assertEquals(RECORD_DATE, viewModel.state.value.deleteTarget?.recordDate)
        }

    @Test
    fun `하루 삭제 성공은 기록과 홈 draft 상태를 초기화하고 완료 확인 뒤 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestDelete)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmDelete)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertEquals(listOf(RECORD_DATE), recordRepository.deletedRecordDates)
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
            updateTimelineEventMemoUseCase =
                UpdateTimelineEventMemoUseCase(
                    repository = recordRepository,
                    sessionRepository = repository,
                    messageHelper = NoOpMessageHelper,
                ),
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
        viewModel.sendIntent(TimelineRecordUiIntent.Initialize(record.recordDate))
        advanceUntilIdle()
        return viewModel
    }

    private fun timeline(events: List<TimelineEvent>) =
        DailyTimeline(
            dailyRecordId = DAILY_RECORD_ID,
            recordDate = RECORD_DATE,
            emotion = null,
            events = events,
        )

    private fun event(memo: String? = null) =
        TimelineEvent(
            timelineEventId = 1L,
            eventType = TimelineEventType.WORK,
            startAt = LocalDateTime.of(2026, 5, 8, 9, 0),
            endAt = null,
            title = "업무",
            subtitle = null,
            memo = memo,
            items = emptyList(),
        )

    private class FakeTimelineRecordSessionRepository : TimelineRecordSessionRepository {
        private val mutableTimeline = MutableStateFlow<DailyTimeline?>(null)
        override val timeline: StateFlow<DailyTimeline?> = mutableTimeline

        override fun save(timeline: DailyTimeline) {
            mutableTimeline.value = timeline
        }

        override fun replaceEvent(event: TimelineEvent) {
            mutableTimeline.value =
                mutableTimeline.value?.copy(
                    events =
                        mutableTimeline.value
                            ?.events
                            .orEmpty()
                            .map { current ->
                                if (current.timelineEventId == event.timelineEventId) event else current
                            },
                )
        }

        override fun removeEvent(timelineEventId: Long) = Unit

        override fun clear() {
            mutableTimeline.value = null
        }
    }

    private class RecordingTimelineRecordRepository : TimelineRecordRepository {
        val requestedRecordDates = mutableListOf<LocalDate>()
        val deletedRecordDates = mutableListOf<LocalDate>()
        val updatedMemos = mutableListOf<Pair<Long, String?>>()
        var dailyRecordResult: Result<DailyTimeline>? = null
        var dailyRecordGate: CompletableDeferred<DailyTimeline>? = null
        var memoUpdateGate: CompletableDeferred<Unit>? = null
        var failure: ApiException? = null

        override suspend fun getDailyRecords(): List<DailyTimeline> = error("사용하지 않음")

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline {
            requestedRecordDates += recordDate
            dailyRecordGate?.let { gate ->
                dailyRecordGate = null
                return gate.await()
            }
            val result = dailyRecordResult ?: error("사용하지 않음")
            return result.getOrThrow()
        }

        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent = error("사용하지 않음")

        override suspend fun updateEventMemo(
            timelineEventId: Long,
            memo: String?,
        ): TimelineEvent {
            updatedMemos += timelineEventId to memo
            memoUpdateGate?.let { gate ->
                memoUpdateGate = null
                gate.await()
            }
            failure?.let { throw it }
            return TimelineEvent(
                timelineEventId = timelineEventId,
                eventType = TimelineEventType.WORK,
                startAt = LocalDateTime.of(2026, 5, 8, 9, 0),
                endAt = null,
                title = "업무",
                subtitle = null,
                memo = memo,
                items = emptyList(),
            )
        }

        override suspend fun deleteEvent(timelineEventId: Long) = error("사용하지 않음")

        override suspend fun deleteDailyRecord(recordDate: LocalDate) {
            failure?.let { throw it }
            deletedRecordDates += recordDate
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
        val RECORD_DATE: LocalDate = LocalDate.of(2026, 5, 8)
        val OTHER_RECORD_DATE: LocalDate = LocalDate.of(2026, 5, 9)
    }
}
