package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.DailyRecordStatus
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.navigation.TimelineEventEditorPage
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import com.soma369.laimory.core.domain.usecase.CompleteDailyRecordUseCase
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineRecordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTimelineRecordSessionRepository()
    private val recordRepository = RecordingTimelineRecordRepository()
    private val draftTaskCoordinator = FakeDraftTaskCoordinator()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingUserMessageHelper()

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
    fun `Event 선택 Intent는 DRAFT 기록에서 선택한 Event 편집 화면으로 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

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
    fun `저장 CTA는 저장 대신 감정 시트를 열고 무덤덤을 기본으로 고른다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            advanceUntilIdle()

            val sheet = viewModel.state.value.emotionSheet
            assertEquals(RECORD_DATE, sheet?.recordDate)
            assertEquals(TimelineEmotion.NEUTRAL, sheet?.selected)
            // 시트를 여는 것만으로는 아무 요청도 나가지 않는다.
            assertTrue(recordRepository.savedRecordDates.isEmpty())
            assertEquals(false, viewModel.state.value.isSavingRecord)
        }

    @Test
    fun `감정을 다시 고르면 선택이 교체되고 그 감정이 저장 요청에 실린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.SelectEmotion(TimelineEmotion.UNHAPPY))
            viewModel.sendIntent(TimelineRecordUiIntent.SelectEmotion(TimelineEmotion.VERY_HAPPY))
            runCurrent()

            assertEquals(TimelineEmotion.VERY_HAPPY, viewModel.state.value.emotionSheet?.selected)

            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            advanceUntilIdle()

            assertEquals(listOf(TimelineEmotion.VERY_HAPPY), recordRepository.savedEmotions)
        }

    @Test
    fun `시트를 닫으면 저장하지 않고 초안과 화면을 그대로 둔다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.SelectEmotion(TimelineEmotion.HAPPY))
            viewModel.sendIntent(TimelineRecordUiIntent.DismissEmotionSheet)
            advanceUntilIdle()

            assertEquals(null, viewModel.state.value.emotionSheet)
            assertTrue(recordRepository.savedRecordDates.isEmpty())
            assertTrue(viewModel.state.value.content is TimelineRecordUiContent.Record)
            assertEquals(0, navigationHelper.backCount)

            // 다시 열면 기본 선택으로 돌아온다.
            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            runCurrent()

            assertEquals(TimelineEmotion.NEUTRAL, viewModel.state.value.emotionSheet?.selected)
        }

    @Test
    fun `저장 중에는 시트를 닫거나 감정을 바꿀 수 없다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            recordRepository.saveGate = gate
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.SelectEmotion(TimelineEmotion.VERY_UNHAPPY))
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            runCurrent()

            viewModel.sendIntent(TimelineRecordUiIntent.SelectEmotion(TimelineEmotion.VERY_HAPPY))
            viewModel.sendIntent(TimelineRecordUiIntent.DismissEmotionSheet)
            runCurrent()

            // 요청이 떠 있는 동안 시트가 사라지거나 선택이 바뀌면 안내와 종결이 갈 곳을 잃는다.
            assertEquals(TimelineEmotion.VERY_UNHAPPY, viewModel.state.value.emotionSheet?.selected)
            assertEquals(true, viewModel.state.value.isSavingRecord)

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf(TimelineEmotion.VERY_UNHAPPY), recordRepository.savedEmotions)
        }

    @Test
    fun `실패한 뒤에는 시트와 선택 감정을 유지해 같은 감정으로 다시 저장한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.saveFailure = ApiException.NetworkException()
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.SelectEmotion(TimelineEmotion.UNHAPPY))
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            advanceUntilIdle()

            assertEquals(TimelineEmotion.UNHAPPY, viewModel.state.value.emotionSheet?.selected)
            assertEquals(false, viewModel.state.value.isSavingRecord)

            recordRepository.saveFailure = null
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            advanceUntilIdle()

            assertEquals(
                listOf(TimelineEmotion.UNHAPPY, TimelineEmotion.UNHAPPY),
                recordRepository.savedEmotions,
            )
        }

    @Test
    fun `시트 안내 문구의 날짜는 오늘 어제 그 밖을 구분한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()
            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            runCurrent()
            assertEquals("오늘", viewModel.state.value.emotionSheet?.dateLabel)

            // 자정을 넘겨 저장하면 같은 초안이 "어제"가 된다.
            clock = Clock.fixed(Instant.parse("2026-05-09T12:00:00Z"), ZoneOffset.UTC)
            val nextDayViewModel = createLoadedViewModel()
            nextDayViewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            runCurrent()
            assertEquals("어제", nextDayViewModel.state.value.emotionSheet?.dateLabel)

            clock = Clock.fixed(Instant.parse("2026-05-20T12:00:00Z"), ZoneOffset.UTC)
            val pastViewModel = createLoadedViewModel()
            pastViewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            runCurrent()
            assertEquals("05.08", pastViewModel.state.value.emotionSheet?.dateLabel)
        }

    @Test
    fun `시트의 확인은 선택한 감정으로 작성 완료를 요청하고 홈으로 복귀한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()
            val effects = mutableListOf<TimelineRecordUiSideEffect>()
            backgroundScope.launch { viewModel.sideEffect.collect(effects::add) }

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            advanceUntilIdle()

            assertEquals(listOf(RECORD_DATE), recordRepository.savedRecordDates)
            // 아무것도 고치지 않고 확인만 누르면 기본 선택(무덤덤)이 그대로 실린다.
            assertEquals(listOf(TimelineEmotion.NEUTRAL), recordRepository.savedEmotions)
            assertEquals(null, viewModel.state.value.emotionSheet)
            assertEquals(1, draftTaskCoordinator.discardCount)
            // 성공 시 화면 상태를 즉시 종결해 중복 요청을 동기 차단하고 잔여 저장 중 상태를 남기지 않는다.
            assertEquals(false, viewModel.state.value.isSavingRecord)
            assertEquals(TimelineRecordUiContent.Unavailable, viewModel.state.value.content)
            assertEquals(1, navigationHelper.backCount)
            // 완료 안내는 화면 pop과 함께 수집이 끊기지 않도록 Root 수명 채널로 보낸다.
            assertEquals(listOf(UserMessage.DailyRecordSaved), messageHelper.sent)
            assertTrue(effects.isEmpty())
        }

    @Test
    fun `저장 완료 후 같은 ViewModel로 재진입하면 서버 기준으로 다시 조회한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()
            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            advanceUntilIdle()
            assertEquals(TimelineRecordUiContent.Unavailable, viewModel.state.value.content)

            // NavDisplay가 엔트리 스코프 ViewModel을 제공하지 않아 재진입 시 같은 인스턴스가 재사용된다.
            recordRepository.dailyRecordResult =
                Result.success(timeline(events = listOf(event()), status = DailyRecordStatus.SAVED))
            viewModel.sendIntent(TimelineRecordUiIntent.Initialize(RECORD_DATE))
            advanceUntilIdle()

            assertEquals(false, viewModel.state.value.isSavingRecord)
            val content = viewModel.state.value.content as TimelineRecordUiContent.Record
            assertEquals(false, content.value.isEditable)
        }

    @Test
    fun `SAVED 기록은 감정 시트를 열지도 저장하지도 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel =
                createLoadedViewModel(
                    record = timeline(events = listOf(event()), status = DailyRecordStatus.SAVED),
                )

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            advanceUntilIdle()

            assertEquals(null, viewModel.state.value.emotionSheet)
            assertTrue(recordRepository.savedRecordDates.isEmpty())
            assertEquals(0, navigationHelper.backCount)
        }

    @Test
    fun `상태를 알 수 없는 기록은 작성 중으로 간주해 저장을 허용한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel =
                createLoadedViewModel(record = timeline(events = listOf(event()), status = null))

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            advanceUntilIdle()

            assertEquals(listOf(RECORD_DATE), recordRepository.savedRecordDates)
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `다른 날짜의 초안 추적은 저장 완료 시 정리하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            draftTaskCoordinator.setActiveTask(RECORD_DATE.plusDays(1))
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            advanceUntilIdle()

            assertEquals(0, draftTaskCoordinator.discardCount)
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `이미 저장된 기록 응답은 작성 완료로 수렴한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.saveFailure =
                ApiException.ConflictException(errorCode = -1003, rawCode = 409)
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            advanceUntilIdle()

            assertEquals(1, draftTaskCoordinator.discardCount)
            assertEquals(1, navigationHelper.backCount)
            assertEquals(listOf(UserMessage.DailyRecordSaved), messageHelper.sent)
        }

    @Test
    fun `기록이 사라진 404는 안내 후 화면을 유지한 채 추적만 정리한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.saveFailure =
                ApiException.ClientException(errorCode = -404, rawCode = 404)
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            advanceUntilIdle()

            assertEquals(1, draftTaskCoordinator.discardCount)
            assertEquals(0, navigationHelper.backCount)
            assertEquals(false, viewModel.state.value.isSavingRecord)
            // 세션 정리로 표시 중이던 기록은 Unavailable로 전환된다.
            assertEquals(TimelineRecordUiContent.Unavailable, viewModel.state.value.content)
            assertEquals(
                TimelineRecordUiSideEffect.ShowSnackbar("이미 삭제됐거나 접근할 수 없는 기록이에요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `네트워크 실패는 초안을 유지한 채 안내하고 다시 저장할 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.saveFailure = ApiException.NetworkException()
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            advanceUntilIdle()

            assertEquals(false, viewModel.state.value.isSavingRecord)
            assertTrue(viewModel.state.value.content is TimelineRecordUiContent.Record)
            assertEquals(0, navigationHelper.backCount)
            assertEquals(
                TimelineRecordUiSideEffect.ShowSnackbar("네트워크 상태를 확인한 뒤 다시 저장해주세요."),
                viewModel.sideEffect.first(),
            )

            recordRepository.saveFailure = null
            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            advanceUntilIdle()

            assertEquals(listOf(RECORD_DATE, RECORD_DATE), recordRepository.savedRecordDates)
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `저장 중 중복 요청을 막는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            recordRepository.saveGate = gate
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            runCurrent()
            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            runCurrent()

            assertEquals(true, viewModel.state.value.isSavingRecord)
            assertEquals(listOf(RECORD_DATE), recordRepository.savedRecordDates)

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf(RECORD_DATE), recordRepository.savedRecordDates)
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `저장 실패 뒤 큐에 쌓인 요청이 자동으로 재실행되지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            recordRepository.saveGate = gate
            recordRepository.saveFailure = ApiException.NetworkException()
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            runCurrent()
            // 저장 I/O가 Intent 루프를 점유하지 않으므로 중복 요청은 큐가 아니라 가드에서 걸러진다.
            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            runCurrent()

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf(RECORD_DATE), recordRepository.savedRecordDates)
            assertEquals(false, viewModel.state.value.isSavingRecord)
            assertEquals(0, navigationHelper.backCount)
        }

    @Test
    fun `저장 중 뒤로가기 요청은 실패 이후에도 화면을 닫지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            recordRepository.saveGate = gate
            recordRepository.saveFailure = ApiException.NetworkException()
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            runCurrent()
            viewModel.sendIntent(TimelineRecordUiIntent.NavigateBack)
            runCurrent()

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(0, navigationHelper.backCount)
        }

    @Test
    fun `초안 추적 정리에 실패해도 저장 결과를 반영하고 이후 Intent를 계속 처리한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            draftTaskCoordinator.discardFailure = IllegalStateException("DataStore 정리 실패")
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            advanceUntilIdle()

            assertEquals(listOf(RECORD_DATE), recordRepository.savedRecordDates)
            assertEquals(false, viewModel.state.value.isSavingRecord)
            assertEquals(TimelineRecordUiContent.Unavailable, viewModel.state.value.content)
            assertEquals(listOf(UserMessage.DailyRecordSaved), messageHelper.sent)
            assertEquals(1, navigationHelper.backCount)

            // Intent 소비 루프가 살아 있어 후속 Intent가 계속 처리된다.
            viewModel.sendIntent(TimelineRecordUiIntent.RetryLoad)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.content is TimelineRecordUiContent.Record)
        }

    @Test
    fun `읽기 전용 기록은 편집과 삭제 진입을 전부 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel =
                createLoadedViewModel(
                    record = timeline(events = listOf(event(memo = "메모")), status = DailyRecordStatus.SAVED),
                )

            viewModel.sendIntent(TimelineRecordUiIntent.SelectEvent(timelineEventId = 1L))
            viewModel.sendIntent(TimelineRecordUiIntent.EditMemo(timelineEventId = 1L))
            viewModel.sendIntent(TimelineRecordUiIntent.RequestDelete)
            runCurrent()

            assertTrue(navigationHelper.pages.isEmpty())
            assertEquals(null, viewModel.state.value.memoEditor)
            assertEquals(TimelineDeleteDialogState.Hidden, viewModel.state.value.deleteDialogState)
        }

    @Test
    fun `저장 중에는 이벤트 선택과 메모 편집을 막는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            recordRepository.saveGate = gate
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(TimelineRecordUiIntent.RequestSave)
            viewModel.sendIntent(TimelineRecordUiIntent.ConfirmEmotion)
            runCurrent()
            viewModel.sendIntent(TimelineRecordUiIntent.SelectEvent(timelineEventId = 1L))
            viewModel.sendIntent(TimelineRecordUiIntent.EditMemo(timelineEventId = 1L))
            runCurrent()

            assertTrue(navigationHelper.pages.isEmpty())
            assertEquals(null, viewModel.state.value.memoEditor)

            gate.complete(Unit)
            advanceUntilIdle()
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

    /** 기록 날짜([RECORD_DATE])가 "오늘"이 되도록 고정한다 — 안내 문구 검증에 기준 시각이 필요하다. */
    private var clock: Clock = Clock.fixed(Instant.parse("2026-05-08T12:00:00Z"), ZoneOffset.UTC)

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
            completeDailyRecordUseCase =
                CompleteDailyRecordUseCase(
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
            messageHelper = messageHelper,
            clock = clock,
        )

    private fun TestScope.createLoadedViewModel(record: DailyTimeline = timeline(events = listOf(event()))): TimelineRecordViewModel {
        recordRepository.dailyRecordResult = Result.success(record)
        val viewModel = createViewModel()
        viewModel.sendIntent(TimelineRecordUiIntent.Initialize(record.recordDate))
        advanceUntilIdle()
        return viewModel
    }

    private fun timeline(
        events: List<TimelineEvent>,
        status: DailyRecordStatus? = DailyRecordStatus.DRAFT,
    ) = DailyTimeline(
        dailyRecordId = DAILY_RECORD_ID,
        recordDate = RECORD_DATE,
        emotion = null,
        events = events,
        status = status,
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

        override fun removeEventItem(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = Unit

        override fun clear() {
            mutableTimeline.value = null
        }
    }

    private class RecordingTimelineRecordRepository : TimelineRecordRepository {
        val requestedRecordDates = mutableListOf<LocalDate>()
        val deletedRecordDates = mutableListOf<LocalDate>()
        val savedRecordDates = mutableListOf<LocalDate>()
        val savedEmotions = mutableListOf<TimelineEmotion>()
        var saveGate: CompletableDeferred<Unit>? = null
        var saveFailure: ApiException? = null
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

        override suspend fun deleteEventPhoto(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = error("사용하지 않음")

        override suspend fun saveDailyRecord(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) {
            savedRecordDates += recordDate
            savedEmotions += emotion
            saveGate?.let { gate ->
                saveGate = null
                gate.await()
            }
            saveFailure?.let { throw it }
        }

        override suspend fun getMonthlyDailyRecords(month: YearMonth): List<MonthlyDailyRecord> = error("사용하지 않음")

        override suspend fun deleteDailyRecord(recordDate: LocalDate) {
            failure?.let { throw it }
            deletedRecordDates += recordDate
        }
    }

    private class RecordingUserMessageHelper : MessageHelper {
        val sent = mutableListOf<UserMessage>()

        override fun send(message: UserMessage) {
            sent += message
        }
    }

    private class FakeDraftTaskCoordinator : DraftTaskCoordinator {
        private val mutableState =
            MutableStateFlow<DraftTaskTrackingState>(
                successState(LocalDate.of(2026, 5, 8)),
            )
        override val state: StateFlow<DraftTaskTrackingState> = mutableState
        var discardCount = 0
        var discardFailure: Throwable? = null

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
            discardFailure?.let { throw it }
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
