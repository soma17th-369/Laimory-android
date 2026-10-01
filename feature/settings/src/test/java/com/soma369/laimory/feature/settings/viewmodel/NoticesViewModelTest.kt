package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.notice.NewNoticePolicy
import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.NoticeRepository
import com.soma369.laimory.core.domain.usecase.notice.GetNoticesUseCase
import com.soma369.laimory.core.domain.usecase.notice.MarkNoticeReadUseCase
import com.soma369.laimory.feature.settings.state.NoticeListContent
import com.soma369.laimory.feature.settings.state.NoticesUiIntent
import com.soma369.laimory.feature.settings.state.NoticesUiSideEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class NoticesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeNoticeRepository()
    private val navigationHelper = RecordingNavigationHelper()

    // 한국 시각 2026-09-28 12:00. 새 공지 기간은 09-21 12:00 부터다.
    private val policy = NewNoticePolicy(Clock.fixed(Instant.parse("2026-09-28T03:00:00Z"), ZoneOffset.UTC))

    @Test
    fun `받기 전에는 불러오는 중이다`() {
        assertEquals(NoticeListContent.Loading, createViewModel().state.value.content)
    }

    @Test
    fun `받은 공지를 서버 순서 그대로, 읽지 않은 최근 공지에 표시를 달아 보여 준다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.result = Result.success(listOf(recent(3), recent(2), old(1)))
            repository.readIds += 2L
            val viewModel = createViewModel()

            viewModel.sendIntent(NoticesUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(
                NoticeListContent.Items(listOf(recent(3), recent(2), old(1)), newIds = setOf(3L)),
                viewModel.state.value.content,
            )
        }

    @Test
    fun `공지가 없으면 빈 상태다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.result = Result.success(emptyList())
            val viewModel = createViewModel()

            viewModel.sendIntent(NoticesUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(NoticeListContent.Empty, viewModel.state.value.content)
        }

    @Test
    fun `처음 받기에 실패하면 다시 시도할 수 있는 실패 상태다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.result = Result.failure(ApiException.NetworkException())
            val viewModel = createViewModel()

            viewModel.sendIntent(NoticesUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(NoticeListContent.LoadFailed, viewModel.state.value.content)
        }

    @Test
    fun `보여 주던 목록은 갱신에 실패해도 지우지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 브라우저에서 원문을 보고 돌아올 때마다 다시 받는다. 그때 망이 끊겼다고 목록이
            // 실패 문구로 바뀌면 방금 보던 것을 잃는다.
            repository.result = Result.success(listOf(old(1)))
            val viewModel = createViewModel()
            viewModel.sendIntent(NoticesUiIntent.Sync)
            advanceUntilIdle()

            repository.result = Result.failure(ApiException.NetworkException())
            viewModel.sendIntent(NoticesUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(NoticeListContent.Items(listOf(old(1))), viewModel.state.value.content)
        }

    @Test
    fun `읽음 기록을 읽지 못해도 목록은 보여 주고 표시만 뺀다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.result = Result.success(listOf(recent(1)))
            repository.readFailure = IllegalStateException("disk")
            val viewModel = createViewModel()

            viewModel.sendIntent(NoticesUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(NoticeListContent.Items(listOf(recent(1))), viewModel.state.value.content)
        }

    @Test
    fun `공지를 누르면 원문 열기를 요청하고, 열렸다는 알림이 와야 그 공지의 표시만 지운다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.result = Result.success(listOf(recent(3), recent(2), old(1)))
            val viewModel = createViewModel()
            viewModel.sendIntent(NoticesUiIntent.Sync)
            advanceUntilIdle()

            viewModel.sendIntent(NoticesUiIntent.NoticeClicked(recent(3)))
            advanceUntilIdle()
            assertEquals(NoticesUiSideEffect.OpenContent(recent(3)), viewModel.sideEffect.first())

            viewModel.sendIntent(NoticesUiIntent.NoticeOpened(recent(3)))
            advanceUntilIdle()

            assertEquals(setOf(2L), (viewModel.state.value.content as NoticeListContent.Items).newIds)
            assertEquals(setOf(3L), repository.readIds)
            // 표시 기간이 지난 공지는 읽음 기록에서 정리 대상이다.
            assertEquals(setOf(3L, 2L), repository.lastKeepIds)
        }

    @Test
    fun `원문을 열지 못하면 읽음으로 남기지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 브라우저가 없어 열기에 실패하면 화면은 열림 알림을 보내지 않는다. 보지 못한 공지의 점이 사라지면 안 된다.
            repository.result = Result.success(listOf(recent(3)))
            val viewModel = createViewModel()
            viewModel.sendIntent(NoticesUiIntent.Sync)
            advanceUntilIdle()

            viewModel.sendIntent(NoticesUiIntent.NoticeClicked(recent(3)))
            advanceUntilIdle()

            assertEquals(setOf(3L), (viewModel.state.value.content as NoticeListContent.Items).newIds)
            assertTrue(repository.readIds.isEmpty())
        }

    @Test
    fun `목록이 보이는 중 새로 받기에 실패하면 알린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.result = Result.success(listOf(old(1)))
            val viewModel = createViewModel()
            viewModel.sendIntent(NoticesUiIntent.Sync)
            advanceUntilIdle()

            repository.result = Result.failure(ApiException.NetworkException())
            viewModel.sendIntent(NoticesUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(
                NoticesUiSideEffect.ShowSnackbar("공지를 새로 불러오지 못했어요. 잠시 후 다시 시도해 주세요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `뒤로 가기는 이전 화면으로 돌아간다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(NoticesUiIntent.NavigateBack)
            advanceUntilIdle()

            assertTrue(navigationHelper.wentBack)
        }

    private fun createViewModel() =
        NoticesViewModel(
            getNoticesUseCase = GetNoticesUseCase(repository, policy, NoOpMessageHelper),
            markNoticeReadUseCase = MarkNoticeReadUseCase(repository, policy),
            navigationHelper = navigationHelper,
        )

    private fun recent(id: Long) = notice(id, LocalDateTime.of(2026, 9, 27, 9, 0))

    private fun old(id: Long) = notice(id, LocalDateTime.of(2026, 9, 1, 9, 0))

    private fun notice(
        id: Long,
        publishedAt: LocalDateTime,
    ) = Notice(
        id = id,
        title = "공지 $id",
        contentUrl = "https://www.laimory.app/notices/$id",
        publishedAt = publishedAt,
    )

    private class FakeNoticeRepository : NoticeRepository {
        var result: Result<List<Notice>> = Result.success(emptyList())
        val readIds = mutableSetOf<Long>()
        var readFailure: Throwable? = null
        var lastKeepIds: Set<Long>? = null

        override suspend fun getNotices(): List<Notice> = result.getOrThrow()

        override suspend fun getReadNoticeIds(): Set<Long> {
            readFailure?.let { throw it }
            return readIds.toSet()
        }

        override suspend fun markRead(
            noticeId: Long,
            keepIds: Set<Long>,
        ) {
            lastKeepIds = keepIds
            readIds.retainAll(keepIds)
            readIds += noticeId
        }
    }

    private data object NoOpMessageHelper : MessageHelper {
        override fun send(message: UserMessage) = Unit
    }

    private class RecordingNavigationHelper : NavigationHelper {
        var wentBack = false

        override fun navigateTo(page: Page) = Unit

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() {
            wentBack = true
        }
    }
}
