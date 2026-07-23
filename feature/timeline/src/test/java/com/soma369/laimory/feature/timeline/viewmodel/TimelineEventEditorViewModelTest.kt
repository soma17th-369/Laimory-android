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
import com.soma369.laimory.core.domain.usecase.ObserveTimelineRecordUseCase
import com.soma369.laimory.core.domain.usecase.UpdateTimelineEventUseCase
import com.soma369.laimory.core.domain.usecase.UploadTimelineEventPhotoUseCase
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorContent
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiSideEffect
import com.soma369.laimory.feature.timeline.state.TimelineEventPhotoUploadState
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
            assertEquals(TimelineEventEditorContent.Editor, state.content)
            assertEquals("출근길", state.form?.title)
            assertEquals("강남역 → 성수역", state.form?.subtitle)
            assertEquals(listOf("https://photo/1.jpg"), state.existingPhotoUrls)
            assertFalse(state.hasUnsavedChanges)
        }

    @Test
    fun `없는 Event는 수정 불가 상태로 표시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(TimelineEventEditorUiIntent.Initialize(999L))
            runCurrent()

            assertEquals(TimelineEventEditorContent.Unavailable, viewModel.state.value.content)
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
            assertEquals(TimelineEventEditorContent.Loading, viewModel.state.value.content)

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
            assertEquals(TimelineEventEditorContent.Loading, viewModel.state.value.content)

            viewModel.sendIntent(TimelineEventEditorUiIntent.Initialize(EVENT_ID))
            runCurrent()

            assertEquals("출근길", viewModel.state.value.form?.title)
            assertFalse(viewModel.state.value.hasUnsavedChanges)
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
            ),
    )

    private inner class RecordingTimelineRecordRepository : TimelineRecordRepository {
        val commands = mutableListOf<UpdateTimelineEventCommand>()

        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent {
            commands += command
            return event(
                title = command.title,
                memo = (command.memo as? TimelineEventUpdateField.Value)?.value,
            )
        }
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

        override fun removeEvent(timelineEventId: Long) = Unit

        override fun clear() {
            mutableTimeline.value = null
        }

        private fun DailyTimeline?.orEmptyEvents(): List<TimelineEvent> = this?.events.orEmpty()
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
