package com.soma369.laimory.feature.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.collection.SourceItemRetentionConfig
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.DraftTaskCompletion
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.feature.home.draft.DraftLoadingSession
import com.soma369.laimory.feature.home.draft.DraftLoadingSessionStore
import com.soma369.laimory.feature.home.loading.DraftLoadingStage
import com.soma369.laimory.feature.home.loading.DraftLoadingStageState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
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

            coordinator.emit(DraftTaskTrackingState.Success(task))
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

            coordinator.emit(DraftTaskTrackingState.Success(task))
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

    private fun createViewModel() =
        DraftLoadingViewModel(
            coordinator = coordinator,
            loadingSessionStore = loadingSessionStore,
            navigationHelper = RecordingNavigationHelper(),
            clock = clock,
            retentionConfig = SourceItemRetentionConfig(RETENTION_DAYS),
        ).also { created = it }

    private class FakeDraftTaskCoordinator : DraftTaskCoordinator {
        private val mutableState = MutableStateFlow<DraftTaskTrackingState>(DraftTaskTrackingState.Idle)
        override val state: StateFlow<DraftTaskTrackingState> = mutableState
        override val completions: Flow<DraftTaskCompletion> = emptyFlow()

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
        override fun navigateTo(page: Page) = Unit

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() = Unit
    }
}
