package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineEventUpdateField
import com.soma369.laimory.core.domain.model.timeline.TimelineItem
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import com.soma369.laimory.core.domain.usecase.DeleteTimelineEventPhotoUseCase
import com.soma369.laimory.core.domain.usecase.DeleteTimelineEventUseCase
import com.soma369.laimory.core.domain.usecase.ObserveTimelineRecordUseCase
import com.soma369.laimory.core.domain.usecase.UpdateTimelineEventUseCase
import com.soma369.laimory.core.domain.usecase.UploadTimelineEventPhotoUseCase
import com.soma369.laimory.feature.timeline.state.TimelineDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiContent
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiSideEffect
import com.soma369.laimory.feature.timeline.state.TimelineEventExistingPhoto
import com.soma369.laimory.feature.timeline.state.TimelineEventPhotoDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineEventPhotoUploadState
import com.soma369.laimory.feature.timeline.state.TimelineEventTimeField
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineEventEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var sessionRepository: FakeTimelineRecordSessionRepository
    private lateinit var recordRepository: RecordingTimelineRecordRepository
    private lateinit var draftRepository: RecordingTimelineDraftRepository
    private lateinit var navigationHelper: RecordingNavigationHelper
    private val messageHelper = NoOpMessageHelper()

    @Before
    fun setUp() {
        sessionRepository = FakeTimelineRecordSessionRepository()
        sessionRepository.save(timeline())
        recordRepository = RecordingTimelineRecordRepository()
        draftRepository = RecordingTimelineDraftRepository()
        navigationHelper = RecordingNavigationHelper()
    }

    @Test
    fun `초기화하면 세션 Event와 기존 사진을 편집 상태로 연다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.Initialize(EVENT_ID))
            runCurrent()

            val state = viewModel.state.value
            assertEquals(TimelineEventEditorUiContent.Editor, state.content)
            assertEquals("출근길", state.form?.title)
            assertEquals("강남역 → 성수역", state.form?.subtitle)
            assertEquals(
                listOf(
                    TimelineEventExistingPhoto(1L, "https://photo/1.jpg"),
                    TimelineEventExistingPhoto(2L, null),
                ),
                state.existingPhotos,
            )
            assertFalse(state.hasUnsavedChanges)
        }

    @Test
    fun `기존 사진은 확인 직후 삭제하고 미저장 폼 변경을 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = initializedViewModel()
            viewModel.sendIntent(TimelineEventEditorUiIntent.ChangeTitle("수정 중인 제목"))

            viewModel.sendIntent(TimelineEventEditorUiIntent.RequestExistingPhotoRemoval(1L))
            runCurrent()
            assertTrue(viewModel.state.value.photoDeleteDialogState is TimelineEventPhotoDeleteDialogState.Confirmation)

            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmExistingPhotoRemoval)
            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmExistingPhotoRemoval)
            advanceUntilIdle()

            assertEquals(listOf(EVENT_ID to 1L), recordRepository.deletedPhotoIds)
            assertEquals(listOf(2L), viewModel.state.value.existingPhotos.map { it.timelineItemId })
            assertEquals(listOf(2L), sessionRepository.eventItems().map(TimelineItem::timelineItemId))
            assertEquals("수정 중인 제목", viewModel.state.value.form?.title)
            assertTrue(viewModel.state.value.hasUnsavedChanges)
            assertEquals(TimelineEventPhotoDeleteDialogState.Hidden, viewModel.state.value.photoDeleteDialogState)
            assertEquals(
                TimelineEventEditorUiSideEffect.ShowSnackbar("사진을 이벤트에서 제거했어요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `사진 삭제 404는 DailyRecord를 재조회해 기존 사진 상태를 동기화한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.photoDeleteFailure = ApiException.ClientException(errorCode = -404, rawCode = 404)
            recordRepository.dailyRecord = timeline().copy(events = listOf(event().copy(items = event().items.drop(1))))
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.RequestExistingPhotoRemoval(1L))
            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmExistingPhotoRemoval)
            advanceUntilIdle()

            assertEquals(LocalDate.of(2026, 5, 8), recordRepository.requestedRecordDate)
            assertEquals(listOf(2L), viewModel.state.value.existingPhotos.map { it.timelineItemId })
            assertEquals(
                TimelineEventEditorUiSideEffect.ShowSnackbar("사진 목록을 최신 상태로 갱신했어요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `작성 완료된 기록의 사진 삭제는 읽기 전용으로 전환한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.photoDeleteFailure = ApiException.ConflictException(errorCode = -1003, rawCode = 409)
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.RequestExistingPhotoRemoval(1L))
            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmExistingPhotoRemoval)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isReadOnly)
            assertEquals(TimelineEventPhotoDeleteDialogState.Hidden, viewModel.state.value.photoDeleteDialogState)
            assertEquals(listOf(1L, 2L), viewModel.state.value.existingPhotos.map { it.timelineItemId })
            assertEquals(
                TimelineEventEditorUiSideEffect.ShowSnackbar("작성 완료된 기록은 수정할 수 없어요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `사진 삭제 네트워크 실패는 사진과 폼을 유지하고 재시도할 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.photoDeleteFailure = ApiException.NetworkException()
            val viewModel = initializedViewModel()
            viewModel.sendIntent(TimelineEventEditorUiIntent.ChangeTitle("수정 중인 제목"))

            viewModel.sendIntent(TimelineEventEditorUiIntent.RequestExistingPhotoRemoval(1L))
            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmExistingPhotoRemoval)
            advanceUntilIdle()

            val failureState = viewModel.state.value.photoDeleteDialogState
            assertTrue(failureState is TimelineEventPhotoDeleteDialogState.RetryableError)
            assertEquals(listOf(1L, 2L), viewModel.state.value.existingPhotos.map { it.timelineItemId })
            assertEquals("수정 중인 제목", viewModel.state.value.form?.title)

            recordRepository.photoDeleteFailure = null
            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmExistingPhotoRemoval)
            advanceUntilIdle()

            assertEquals(listOf(EVENT_ID to 1L), recordRepository.deletedPhotoIds)
            assertEquals(listOf(2L), viewModel.state.value.existingPhotos.map { it.timelineItemId })
            assertEquals("수정 중인 제목", viewModel.state.value.form?.title)
        }

    @Test
    fun `사진 삭제 404 재조회에서 Event가 없으면 수정 불가 상태로 전환한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.photoDeleteFailure = ApiException.ClientException(errorCode = -404, rawCode = 404)
            recordRepository.dailyRecord = timeline().copy(events = emptyList())
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.RequestExistingPhotoRemoval(1L))
            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmExistingPhotoRemoval)
            advanceUntilIdle()

            assertEquals(TimelineEventEditorUiContent.Unavailable, viewModel.state.value.content)
            assertEquals(TimelineEventPhotoDeleteDialogState.Hidden, viewModel.state.value.photoDeleteDialogState)
            assertEquals(
                TimelineEventEditorUiSideEffect.ShowSnackbar("이미 삭제됐거나 접근할 수 없는 이벤트예요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `없는 Event는 수정 불가 상태로 표시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.Initialize(999L))
            runCurrent()

            assertEquals(TimelineEventEditorUiContent.Unavailable, viewModel.state.value.content)
        }

    @Test
    fun `필수값 검증 실패 시 PATCH를 호출하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.ChangeTitle("   "))
            runCurrent()

            assertFalse(viewModel.state.value.isSaveEnabled)
            assertEquals("제목을 입력해 주세요.", viewModel.state.value.validation.titleError)

            viewModel.sendIntent(TimelineEventEditorUiIntent.Save)
            advanceUntilIdle()

            assertTrue(recordRepository.commands.isEmpty())
            assertNotNull(viewModel.state.value.validation.titleError)
            assertEquals(TimelineEventEditorUiSideEffect.FocusTitle, viewModel.sideEffect.first())
        }

    @Test
    fun `변경사항 없이 저장하면 PATCH를 호출하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.Save)
            advanceUntilIdle()

            assertTrue(recordRepository.commands.isEmpty())
            assertEquals(0, navigationHelper.backCount)
        }

    @Test
    fun `종료 시각이 시작 시각보다 이르면 익일로 해석한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = initializedViewModel()

            viewModel.sendIntent(
                TimelineEventEditorUiIntent.SelectTime(
                    field = TimelineEventTimeField.START,
                    time = LocalTime.of(23, 0),
                ),
            )
            viewModel.sendIntent(TimelineEventEditorUiIntent.ClearEndTime)
            viewModel.sendIntent(
                TimelineEventEditorUiIntent.SelectTime(
                    field = TimelineEventTimeField.END,
                    time = LocalTime.of(7, 0),
                ),
            )
            runCurrent()

            assertEquals(
                LocalDateTime.of(2026, 5, 9, 7, 0),
                viewModel.state.value.form?.endAt,
            )
            assertEquals(null, viewModel.state.value.validation.timeError)
        }

    @Test
    fun `사진을 업로드한 뒤 통합 PATCH로 필드와 PHOTO를 함께 저장한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.ChangeTitle("수정된 출근길"))
            viewModel.sendIntent(TimelineEventEditorUiIntent.ChangeMemo("  원문 메모  "))
            viewModel.sendIntent(TimelineEventEditorUiIntent.AddPhotos(listOf("content://photo/a", "content://photo/b")))
            viewModel.sendIntent(TimelineEventEditorUiIntent.Save)
            advanceUntilIdle()

            assertEquals(listOf("content://photo/a", "content://photo/b"), draftRepository.uploadedUris)
            assertEquals(1, recordRepository.commands.size)
            val command = recordRepository.commands.single()
            assertEquals("수정된 출근길", command.title)
            assertEquals(TimelineEventUpdateField.Value("  원문 메모  "), command.memo)
            val photos = (command.photosToAdd as TimelineEventUpdateField.Value).value
            assertEquals(2, photos.size)
            assertEquals("uploaded-a.jpg", photos[0].filename)
            assertEquals("content://photo/b", photos[1].clientPhotoUri)
            assertEquals(1, navigationHelper.backCount)
            assertEquals(TimelineEventEditorUiContent.Loading, viewModel.state.value.content)

            viewModel.sendIntent(TimelineEventEditorUiIntent.Initialize(EVENT_ID))
            runCurrent()

            assertEquals("수정된 출근길", viewModel.state.value.form?.title)
        }

    @Test
    fun `사진 업로드 실패 후 재시도는 성공 filename을 보존하고 실패 사진만 다시 업로드한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            draftRepository.failOnceUri = "content://photo/b"
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.AddPhotos(listOf("content://photo/a", "content://photo/b")))
            viewModel.sendIntent(TimelineEventEditorUiIntent.Save)
            advanceUntilIdle()

            assertTrue(recordRepository.commands.isEmpty())
            val firstAttemptPhotos = viewModel.state.value.pendingPhotos
            assertEquals(TimelineEventPhotoUploadState.UPLOADED, firstAttemptPhotos[0].uploadState)
            assertEquals(TimelineEventPhotoUploadState.FAILED, firstAttemptPhotos[1].uploadState)

            viewModel.sendIntent(TimelineEventEditorUiIntent.Save)
            advanceUntilIdle()

            assertEquals(1, draftRepository.uploadCounts.getValue("content://photo/a"))
            assertEquals(2, draftRepository.uploadCounts.getValue("content://photo/b"))
            assertEquals(1, recordRepository.commands.size)
        }

    @Test
    fun `작성 완료 오류는 읽기 전용으로 전환하고 이후 편집을 차단한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.failure = ApiException.ConflictException(errorCode = -1003)
            val viewModel = initializedViewModel()
            viewModel.sendIntent(TimelineEventEditorUiIntent.ChangeTitle("저장 시도"))

            viewModel.sendIntent(TimelineEventEditorUiIntent.Save)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isReadOnly)
            assertFalse(viewModel.state.value.isSaveEnabled)
            assertEquals(
                TimelineEventEditorUiSideEffect.ShowSnackbar("작성 완료된 기록은 수정할 수 없어요."),
                viewModel.sideEffect.first(),
            )

            viewModel.sendIntent(TimelineEventEditorUiIntent.ChangeTitle("차단되어야 할 수정"))
            runCurrent()

            assertEquals("저장 시도", viewModel.state.value.form?.title)
        }

    @Test
    fun `수정 불가 Event 오류는 사용할 수 없는 화면으로 전환한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.failure = ApiException.ClientException(errorCode = -404)
            val viewModel = initializedViewModel()
            viewModel.sendIntent(TimelineEventEditorUiIntent.ChangeTitle("저장 시도"))

            viewModel.sendIntent(TimelineEventEditorUiIntent.Save)
            advanceUntilIdle()

            assertEquals(TimelineEventEditorUiContent.Unavailable, viewModel.state.value.content)
            assertFalse(viewModel.state.value.isSaving)
        }

    @Test
    fun `연속 저장 Intent는 mutation을 한 번만 실행한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = initializedViewModel()
            viewModel.sendIntent(TimelineEventEditorUiIntent.ChangeTitle("수정"))

            viewModel.sendIntent(TimelineEventEditorUiIntent.Save)
            viewModel.sendIntent(TimelineEventEditorUiIntent.Save)
            advanceUntilIdle()

            assertEquals(1, recordRepository.commands.size)
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `변경 후 뒤로가기는 확인을 거쳐 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.ChangeTitle("수정"))
            viewModel.sendIntent(TimelineEventEditorUiIntent.NavigateBack)
            runCurrent()

            assertTrue(viewModel.state.value.isDiscardDialogVisible)
            assertEquals(0, navigationHelper.backCount)

            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmDiscard)
            runCurrent()

            assertEquals(1, navigationHelper.backCount)
            assertEquals(TimelineEventEditorUiContent.Loading, viewModel.state.value.content)

            viewModel.sendIntent(TimelineEventEditorUiIntent.Initialize(EVENT_ID))
            runCurrent()

            assertEquals("출근길", viewModel.state.value.form?.title)
            assertFalse(viewModel.state.value.hasUnsavedChanges)
        }

    @Test
    fun `Event 삭제는 확인 후 한 번만 실행하고 완료 확인 뒤 이전 화면으로 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.RequestDelete)
            runCurrent()
            assertEquals(TimelineDeleteDialogState.Confirmation, viewModel.state.value.deleteDialogState)

            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmDelete)
            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertEquals(listOf(EVENT_ID), recordRepository.deletedEventIds)
            assertTrue(sessionRepository.timeline.value?.events.orEmpty().isEmpty())
            assertEquals(TimelineDeleteDialogState.Success, viewModel.state.value.deleteDialogState)
            assertEquals(0, navigationHelper.backCount)

            viewModel.sendIntent(TimelineEventEditorUiIntent.FinishDelete)
            runCurrent()

            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `사진 삭제 실패는 Event를 유지하고 다이얼로그에서 재시도할 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.failure = ApiException.ServerException(errorCode = -1017, rawCode = 502)
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.RequestDelete)
            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.deleteDialogState is TimelineDeleteDialogState.RetryableError)
            assertEquals(listOf(EVENT_ID), sessionRepository.timeline.value?.events?.map(TimelineEvent::timelineEventId))

            recordRepository.failure = null
            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertEquals(TimelineDeleteDialogState.Success, viewModel.state.value.deleteDialogState)
            assertTrue(sessionRepository.timeline.value?.events.orEmpty().isEmpty())
        }

    @Test
    fun `삭제 확인을 취소한 뒤 들어온 확인 Intent는 Event를 삭제하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.RequestDelete)
            viewModel.sendIntent(TimelineEventEditorUiIntent.DismissDelete)
            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertTrue(recordRepository.deletedEventIds.isEmpty())
            assertEquals(TimelineDeleteDialogState.Hidden, viewModel.state.value.deleteDialogState)
            assertEquals(listOf(EVENT_ID), sessionRepository.timeline.value?.events?.map(TimelineEvent::timelineEventId))
        }

    @Test
    fun `이미 없는 Event 삭제는 사용할 수 없는 상태로 전환하고 안내한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.failure =
                ApiException.ClientException(
                    errorCode = -404,
                    rawCode = 404,
                )
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.RequestDelete)
            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertEquals(TimelineEventEditorUiContent.Unavailable, viewModel.state.value.content)
            assertEquals(TimelineDeleteDialogState.Hidden, viewModel.state.value.deleteDialogState)
            assertEquals(
                TimelineEventEditorUiSideEffect.ShowSnackbar("이미 삭제됐거나 접근할 수 없는 이벤트예요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `작성 완료된 기록의 Event 삭제는 읽기 전용으로 전환하고 안내한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.failure =
                ApiException.ConflictException(
                    errorCode = -1003,
                    rawCode = 409,
                )
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.RequestDelete)
            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isReadOnly)
            assertEquals(TimelineDeleteDialogState.Hidden, viewModel.state.value.deleteDialogState)
            assertEquals(
                TimelineEventEditorUiSideEffect.ShowSnackbar("작성 완료된 기록은 삭제할 수 없어요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `공통 정책으로 처리된 Event 삭제 실패는 추가 오류 다이얼로그 없이 닫는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.failure = ApiException.ServerException(rawCode = 500)
            val viewModel = initializedViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.RequestDelete)
            viewModel.sendIntent(TimelineEventEditorUiIntent.ConfirmDelete)
            advanceUntilIdle()

            assertEquals(TimelineDeleteDialogState.Hidden, viewModel.state.value.deleteDialogState)
            assertEquals(listOf(EVENT_ID), sessionRepository.timeline.value?.events?.map(TimelineEvent::timelineEventId))
        }

    private fun TestScope.initializedViewModel(): TimelineEventEditorViewModel =
        createViewModel().also {
            it.sendIntent(TimelineEventEditorUiIntent.Initialize(EVENT_ID))
            runCurrent()
        }

    private fun createViewModel() =
        TimelineEventEditorViewModel(
            observeTimelineRecordUseCase = ObserveTimelineRecordUseCase(sessionRepository),
            uploadTimelineEventPhotoUseCase =
                UploadTimelineEventPhotoUseCase(
                    repository = draftRepository,
                    messageHelper = messageHelper,
                ),
            updateTimelineEventUseCase =
                UpdateTimelineEventUseCase(
                    repository = recordRepository,
                    sessionRepository = sessionRepository,
                    messageHelper = messageHelper,
                ),
            deleteTimelineEventUseCase =
                DeleteTimelineEventUseCase(
                    repository = recordRepository,
                    sessionRepository = sessionRepository,
                    messageHelper = messageHelper,
                ),
            deleteTimelineEventPhotoUseCase =
                DeleteTimelineEventPhotoUseCase(
                    repository = recordRepository,
                    sessionRepository = sessionRepository,
                    messageHelper = messageHelper,
                ),
            navigationHelper = navigationHelper,
        )

    private fun timeline() =
        DailyTimeline(
            dailyRecordId = 31L,
            recordDate = LocalDate.of(2026, 5, 8),
            emotion = null,
            events = listOf(event()),
        )

    private fun event(
        title: String = "출근길",
        memo: String? = null,
    ) = TimelineEvent(
        timelineEventId = EVENT_ID,
        eventType = TimelineEventType.MOVEMENT,
        startAt = LocalDateTime.of(2026, 5, 8, 8, 30),
        endAt = LocalDateTime.of(2026, 5, 8, 9, 0),
        title = title,
        subtitle = "강남역 → 성수역",
        memo = memo,
        items =
            listOf(
                TimelineItem(
                    timelineItemId = 1L,
                    itemType = TimelineItemType.PHOTO,
                    rawId = "existing-photo",
                    startAt = LocalDateTime.of(2026, 5, 8, 8, 40),
                    endAt = null,
                    photoUrl = "https://photo/1.jpg",
                ),
                TimelineItem(
                    timelineItemId = 2L,
                    itemType = TimelineItemType.PHOTO,
                    rawId = "existing-photo-without-url",
                    startAt = LocalDateTime.of(2026, 5, 8, 8, 45),
                    endAt = null,
                    photoUrl = null,
                ),
            ),
    )

    private inner class RecordingTimelineRecordRepository : TimelineRecordRepository {
        val commands = mutableListOf<UpdateTimelineEventCommand>()
        val deletedEventIds = mutableListOf<Long>()
        val deletedPhotoIds = mutableListOf<Pair<Long, Long>>()
        var failure: ApiException? = null
        var photoDeleteFailure: ApiException? = null
        var dailyRecordFailure: ApiException? = null
        var dailyRecord: DailyTimeline = timeline()
        var requestedRecordDate: LocalDate? = null

        override suspend fun getDailyRecords(): List<DailyTimeline> = error("사용하지 않음")

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline {
            requestedRecordDate = recordDate
            dailyRecordFailure?.let { throw it }
            return dailyRecord
        }

        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent {
            commands += command
            failure?.let { throw it }
            return event(
                title = command.title,
                memo = (command.memo as? TimelineEventUpdateField.Value)?.value,
            )
        }

        override suspend fun updateEventMemo(
            timelineEventId: Long,
            memo: String?,
        ): TimelineEvent = error("사용하지 않음")

        override suspend fun deleteEvent(timelineEventId: Long) {
            failure?.let { throw it }
            deletedEventIds += timelineEventId
        }

        override suspend fun deleteEventPhoto(
            timelineEventId: Long,
            timelineItemId: Long,
        ) {
            photoDeleteFailure?.let { throw it }
            deletedPhotoIds += timelineEventId to timelineItemId
        }

        override suspend fun saveDailyRecord(recordDate: LocalDate) = error("사용하지 않음")

        override suspend fun deleteDailyRecord(recordDate: LocalDate) = Unit
    }

    private class FakeTimelineRecordSessionRepository : TimelineRecordSessionRepository {
        private val mutableTimeline = MutableStateFlow<DailyTimeline?>(null)
        override val timeline: StateFlow<DailyTimeline?> = mutableTimeline

        override fun save(timeline: DailyTimeline) {
            mutableTimeline.value = timeline
        }

        override fun replaceEvent(event: TimelineEvent) {
            mutableTimeline.value =
                mutableTimeline.value?.copy(
                    events = mutableTimeline.value.orEmptyEvents().map { if (it.timelineEventId == event.timelineEventId) event else it },
                )
        }

        override fun removeEvent(timelineEventId: Long) {
            mutableTimeline.value =
                mutableTimeline.value?.copy(
                    events = mutableTimeline.value.orEmptyEvents().filterNot { it.timelineEventId == timelineEventId },
                )
        }

        override fun removeEventItem(
            timelineEventId: Long,
            timelineItemId: Long,
        ) {
            mutableTimeline.value =
                mutableTimeline.value?.copy(
                    events =
                        mutableTimeline.value.orEmptyEvents().map { event ->
                            if (event.timelineEventId == timelineEventId) {
                                event.copy(items = event.items.filterNot { it.timelineItemId == timelineItemId })
                            } else {
                                event
                            }
                        },
                )
        }

        override fun clear() {
            mutableTimeline.value = null
        }

        private fun DailyTimeline?.orEmptyEvents(): List<TimelineEvent> = this?.events.orEmpty()

        fun eventItems(): List<TimelineItem> = timeline.value?.events?.single()?.items.orEmpty()
    }

    private class RecordingTimelineDraftRepository : TimelineDraftRepository {
        val uploadedUris = mutableListOf<String>()
        val uploadCounts = mutableMapOf<String, Int>()
        var failOnceUri: String? = null

        override suspend fun uploadPhotos(clientPhotoUris: List<String>): List<String> =
            clientPhotoUris.map { uri ->
                uploadedUris += uri
                val count = uploadCounts.getOrDefault(uri, 0) + 1
                uploadCounts[uri] = count
                if (uri == failOnceUri && count == 1) throw ApiException.NetworkException()
                "uploaded-${uri.substringAfterLast('/')}.jpg"
            }

        override suspend fun createDraft(
            recordDate: LocalDate,
            zone: ZoneId,
            window: RecordDateWindow,
            items: List<SourceItem>,
            uploadedPhotoFilenames: Map<String, String>,
        ): DraftTaskHandle = error("사용하지 않음")

        override suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot = error("사용하지 않음")
    }

    private class RecordingNavigationHelper : com.soma369.laimory.core.domain.helper.NavigationHelper {
        var backCount = 0

        override fun navigateTo(page: Page) = Unit

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() {
            backCount++
        }
    }

    private class NoOpMessageHelper : MessageHelper {
        override fun send(message: UserMessage) = Unit
    }

    private companion object {
        const val EVENT_ID = 17L
    }
}
