package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import com.soma369.laimory.core.domain.usecase.ObserveTimelineRecordUseCase
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiContent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineRecordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTimelineRecordSessionRepository()
    private val navigationHelper = RecordingNavigationHelper()

    @Test
    fun `결과가 없으면 복구 불가 상태를 표시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            runCurrent()

            assertEquals(TimelineRecordUiContent.Unavailable, viewModel.state.value.content)
        }

    @Test
    fun `저장된 결과와 nullable 필드를 화면 상태로 전달한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.save(timeline(events = listOf(event())))
            val viewModel = createViewModel()

            runCurrent()

            val content = viewModel.state.value.content as TimelineRecordUiContent.Record
            assertEquals(LocalDate.of(2026, 5, 8), content.value.recordDate)
            assertEquals(null, content.value.events.single().endAt)
            assertEquals(null, content.value.events.single().subtitle)
            assertEquals(null, content.value.events.single().memo)
        }

    @Test
    fun `Event가 없는 DailyTimeline은 결과 없음과 구분해 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.save(timeline(events = emptyList()))
            val viewModel = createViewModel()

            runCurrent()

            val content = viewModel.state.value.content as TimelineRecordUiContent.Record
            assertTrue(content.value.events.isEmpty())
        }

    @Test
    fun `결과가 초기화되면 stale 기록 대신 복구 불가 상태로 돌아간다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.save(timeline(events = listOf(event())))
            val viewModel = createViewModel()
            runCurrent()

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

    private fun createViewModel() =
        TimelineRecordViewModel(
            observeTimelineRecordUseCase = ObserveTimelineRecordUseCase(repository),
            navigationHelper = navigationHelper,
        )

    private fun timeline(events: List<TimelineEvent>) =
        DailyTimeline(
            dailyRecordId = 31L,
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

    private class RecordingNavigationHelper : NavigationHelper {
        var backCount = 0

        override fun navigateTo(page: Page) = Unit

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() {
            backCount++
        }
    }
}
