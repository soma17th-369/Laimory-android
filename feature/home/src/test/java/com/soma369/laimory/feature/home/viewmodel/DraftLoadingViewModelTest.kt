package com.soma369.laimory.feature.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.collection.SourceItemRetentionConfig
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.DraftTaskCompletion
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.feature.home.draft.DraftLoadingSession
import com.soma369.laimory.feature.home.draft.DraftLoadingSessionStore
import com.soma369.laimory.feature.home.draft.DraftSubmission
import com.soma369.laimory.feature.home.draft.DraftSubmissionStore
import com.soma369.laimory.feature.home.loading.DraftLoadingStage
import com.soma369.laimory.feature.home.loading.DraftLoadingStageState
import com.soma369.laimory.feature.home.loading.DraftLoadingUiIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DraftLoadingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val date = LocalDate.of(2026, 8, 19)
    private val requestedAt = Instant.parse("2026-08-19T00:00:00Z")
    private val clock = Clock.fixed(requestedAt, ZoneId.of("UTC"))
    private val task = ActiveDraftTask(taskId = "task-1", recordDate = date, requestedAt = requestedAt)

    private val coordinator = FakeDraftTaskCoordinator()
    private val loadingSessionStore = DraftLoadingSessionStore()
    private val submissionStore = DraftSubmissionStore()
    private val navigationHelper = RecordingNavigationHelper()
    private var created: DraftLoadingViewModel? = null

    /**
     * 연출 틱은 작업이 끝날 때까지 도는 무한 루프다. 남겨 두면 `runTest` 가 스케줄러를 비우지 못해
     * 반환하지 않으므로, 본문이 끝나면 실패했더라도 반드시 끊는다.
     */
    private fun loadingTest(body: suspend TestScope.() -> Unit) =
        runTest(mainDispatcherRule.testDispatcher) {
            try {
                body()
            } finally {
                created?.viewModelScope?.cancel()
            }
        }

    @Test
    fun `작업이 끝나 스냅샷을 지워도 화면은 사진과 건수를 유지한다`() =
        loadingTest {
            loadingSessionStore.start(
                DraftLoadingSession(
                    taskId = "task-1",
                    recordDate = date,
                    photoUris = listOf("content://photo/1", "content://photo/2"),
                    photoCount = 2,
                    calendarCount = 4,
                    stayCount = 3,
                ),
            )
            coordinator.emit(DraftTaskTrackingState.Processing(task))
            val viewModel = createViewModel()
            runCurrent()
            assertEquals(2, viewModel.state.value.photoUris.size)

            coordinator.emit(DraftTaskTrackingState.Success(task, eventCount = 1))
            runCurrent()

            // 완료 표시를 보여주는 동안 사진이 사라지거나 `0장 완료`가 되면 안 된다.
            assertEquals(2, viewModel.state.value.photoUris.size)
            assertEquals(2, viewModel.state.value.photoCount)
            assertEquals(4, viewModel.state.value.calendarCount)
            assertEquals(3, viewModel.state.value.stayCount)
            assertEquals(null, loadingSessionStore.session.value)
        }

    @Test
    fun `완료는 연출 틱을 기다리지 않고 모든 줄에 바로 반영된다`() =
        loadingTest {
            coordinator.emit(DraftTaskTrackingState.Processing(task))
            val viewModel = createViewModel()
            runCurrent()

            coordinator.emit(DraftTaskTrackingState.Success(task, eventCount = 1))
            runCurrent()

            DraftLoadingStage.entries.forEach { stage ->
                assertEquals(DraftLoadingStageState.DONE, viewModel.state.value.stageStates[stage])
            }
        }

    @Test
    fun `서버가 알려준 경과 시간이 기기 시각보다 앞서면 그쪽을 따른다`() =
        loadingTest {
            // 기기 시각으로는 0초지만 서버는 이미 20초가 지났다고 알려준다.
            coordinator.emit(DraftTaskTrackingState.Processing(task, elapsedSeconds = 20L))
            val viewModel = createViewModel()
            runCurrent()

            val states = viewModel.state.value.stageStates
            assertEquals(DraftLoadingStageState.DONE, states[DraftLoadingStage.PHOTO])
            assertEquals(DraftLoadingStageState.DONE, states[DraftLoadingStage.CALENDAR])
            assertEquals(DraftLoadingStageState.IN_PROGRESS, states[DraftLoadingStage.STAY])
        }

    @Test
    fun `안내 문구에 쓸 보존 일수는 설정값을 그대로 따른다`() =
        loadingTest {
            // 빌드마다 값이 달라 문구에 숫자를 박을 수 없다 — 설정값이 화면까지 와야 한다.
            val viewModel = createViewModel()
            runCurrent()

            assertEquals(RETENTION_DAYS, viewModel.state.value.retentionDays)
        }

    @Test
    fun `다음 작업이 오면 단계 연출을 처음부터 다시 한다`() =
        loadingTest {
            // 이 ViewModel 은 Activity 범위라 로딩 화면을 다시 열어도 새로 만들어지지 않는다. 연출이 이전
            // 작업에서 멈추면 다음 작업의 로딩 화면이 모두 완료인 채로 뜬다.
            coordinator.emit(DraftTaskTrackingState.Processing(task))
            val viewModel = createViewModel()
            runCurrent()
            coordinator.emit(DraftTaskTrackingState.Success(task, eventCount = 1))
            runCurrent()

            val next = ActiveDraftTask(taskId = "task-2", recordDate = date, requestedAt = requestedAt)
            coordinator.emit(DraftTaskTrackingState.Processing(next))
            runCurrent()

            val states = viewModel.state.value.stageStates
            assertEquals(DraftLoadingStageState.IN_PROGRESS, states[DraftLoadingStage.PHOTO])
            assertEquals(DraftLoadingStageState.PENDING, states[DraftLoadingStage.CALENDAR])
            assertEquals(DraftLoadingStageState.PENDING, states[DraftLoadingStage.STAY])
        }

    @Test
    fun `요청을 보내는 동안은 만들기 누른 시각부터 연출하고 이전 작업의 완료를 보지 않는다`() =
        loadingTest {
            // 다른 날짜의 이전 작업이 완료로 남아 있다. 새 요청의 로딩 화면이 그것을 보면 곧바로 완료가 뜬다.
            coordinator.emit(DraftTaskTrackingState.Success(task.copy(taskId = "old"), eventCount = 3))
            submissionStore.begin(date, requestedAt.minusSeconds(10))
            loadingSessionStore.start(
                DraftLoadingSession(
                    taskId = null,
                    recordDate = date,
                    photoUris = listOf("content://photo/1"),
                    photoCount = 1,
                    calendarCount = 2,
                    stayCount = 0,
                    submittedAt = requestedAt.minusSeconds(10),
                ),
            )
            val viewModel = createViewModel()
            runCurrent()

            val state = viewModel.state.value
            assertEquals(1, state.photoUris.size)
            assertEquals(2, state.calendarCount)
            assertEquals(null, state.notice)
            assertEquals(DraftLoadingStageState.DONE, state.stageStates[DraftLoadingStage.PHOTO])
            assertEquals(DraftLoadingStageState.IN_PROGRESS, state.stageStates[DraftLoadingStage.CALENDAR])
            assertEquals(DraftLoadingStageState.PENDING, state.stageStates[DraftLoadingStage.AI])
        }

    @Test
    fun `작업 번호를 받은 뒤에도 요청 중에 흐른 연출이 되돌아가지 않는다`() =
        loadingTest {
            // 요청에 20초가 걸렸다. 작업의 요청 시각으로만 세면 0초부터 다시 시작한다.
            loadingSessionStore.start(
                DraftLoadingSession(
                    taskId = "task-1",
                    recordDate = date,
                    photoUris = emptyList(),
                    photoCount = 0,
                    calendarCount = 0,
                    stayCount = 0,
                    submittedAt = requestedAt.minusSeconds(20),
                ),
            )
            coordinator.emit(DraftTaskTrackingState.Processing(task))
            val viewModel = createViewModel()
            runCurrent()

            val states = viewModel.state.value.stageStates
            assertEquals(DraftLoadingStageState.DONE, states[DraftLoadingStage.PHOTO])
            assertEquals(DraftLoadingStageState.DONE, states[DraftLoadingStage.CALENDAR])
            assertEquals(DraftLoadingStageState.IN_PROGRESS, states[DraftLoadingStage.STAY])
        }

    @Test
    fun `요청이 무응답으로 끝나면 사유와 홈으로 버튼을 보여 주고 누르면 확인한 채 돌아간다`() =
        loadingTest {
            submissionStore.begin(date, requestedAt)
            val viewModel = createViewModel()
            runCurrent()

            submissionStore.fail(date, SocketTimeoutException())
            runCurrent()

            val notice = viewModel.state.value.notice
            assertEquals("응답이 없어 요청을 멈췄어요. 잠시 후 다시 시도해 주세요.", notice?.message)
            assertEquals("홈으로", notice?.primaryAction?.label)
            // 자동으로 돌아가지 않는다.
            assertEquals(0, navigationHelper.backCount)

            viewModel.sendIntent(DraftLoadingUiIntent.LeaveAfterSubmitFailure)
            runCurrent()

            assertEquals(1, navigationHelper.backCount)
            assertTrue((submissionStore.submission.value as DraftSubmission.Failed).shownOnLoading)
        }

    @Test
    fun `약관 동의가 필요하면 약관 확인하기 버튼을 보여 준다`() =
        loadingTest {
            submissionStore.begin(date, requestedAt)
            val viewModel = createViewModel()
            submissionStore.fail(date, ApiException.ClientException(rawCode = 403, errorCode = -3001, message = "약관"))
            runCurrent()

            assertEquals("약관 확인하기", viewModel.state.value.notice?.primaryAction?.label)
        }

    @Test
    fun `연결이 없어 요청을 못 보냈으면 인터넷 연결 안내를 보여 준다`() =
        loadingTest {
            submissionStore.begin(date, requestedAt)
            val viewModel = createViewModel()
            submissionStore.fail(date, ApiException.NetworkException())
            runCurrent()

            assertEquals("인터넷에 연결되어 있지 않아요. 연결을 확인하고 다시 시도해 주세요.", viewModel.state.value.notice?.message)
            assertEquals("홈으로", viewModel.state.value.notice?.primaryAction?.label)
        }

    @Test
    fun `화면에 들어오고 나가는 것을 저장소에 알린다`() =
        loadingTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftLoadingUiIntent.ChangeVisibility(shown = true))
            runCurrent()
            assertTrue(submissionStore.isLoadingShown.value)

            viewModel.sendIntent(DraftLoadingUiIntent.ChangeVisibility(shown = false))
            runCurrent()
            assertFalse(submissionStore.isLoadingShown.value)
        }

    private fun createViewModel() =
        DraftLoadingViewModel(
            coordinator = coordinator,
            loadingSessionStore = loadingSessionStore,
            submissionStore = submissionStore,
            navigationHelper = navigationHelper,
            clock = clock,
            retentionConfig = SourceItemRetentionConfig(RETENTION_DAYS),
        ).also { created = it }

    private class FakeDraftTaskCoordinator : DraftTaskCoordinator {
        private val mutableState = MutableStateFlow<DraftTaskTrackingState>(DraftTaskTrackingState.Idle)
        override val state: StateFlow<DraftTaskTrackingState> = mutableState
        override val pendingCompletion: StateFlow<DraftTaskCompletion?> = MutableStateFlow(null)

        override suspend fun consumeCompletion(taskId: String): Boolean = false

        fun emit(next: DraftTaskTrackingState) {
            mutableState.value = next
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

        override suspend fun discard() = Unit
    }

    private companion object {
        const val RETENTION_DAYS = 30
    }

    private class RecordingNavigationHelper : NavigationHelper {
        var backCount = 0

        override fun navigateTo(page: Page) = Unit

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() {
            backCount++
        }
    }
}
