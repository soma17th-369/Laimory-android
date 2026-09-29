package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.inquiry.InquiryStatus
import com.soma369.laimory.core.domain.navigation.InquiryDetailPage
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.usecase.inquiry.GetMyInquiriesUseCase
import com.soma369.laimory.feature.settings.state.InquiriesUiIntent
import com.soma369.laimory.feature.settings.state.InquiryListContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InquiriesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeInquiryHistoryRepository()
    private val navigationHelper = RecordingInquiryNavigationHelper()

    @Test
    fun `받은 문의를 서버 순서 그대로 보여 준다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val inquiries = listOf(inquirySummary(2), inquirySummary(1, InquiryStatus.ANSWERED))
            repository.list = Result.success(inquiries)
            val viewModel = createViewModel()

            viewModel.sendIntent(InquiriesUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(InquiryListContent.Items(inquiries), viewModel.state.value.content)
        }

    @Test
    fun `보낸 문의가 없으면 빈 상태다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(InquiriesUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(InquiryListContent.Empty, viewModel.state.value.content)
        }

    @Test
    fun `처음 받기에 실패하면 실패 상태, 보여 주던 목록은 갱신 실패로 지우지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.list = Result.failure(ApiException.NetworkException())
            val viewModel = createViewModel()
            viewModel.sendIntent(InquiriesUiIntent.Sync)
            advanceUntilIdle()
            assertEquals(InquiryListContent.LoadFailed, viewModel.state.value.content)

            repository.list = Result.success(listOf(inquirySummary(1)))
            viewModel.sendIntent(InquiriesUiIntent.Sync)
            advanceUntilIdle()
            repository.list = Result.failure(ApiException.NetworkException())
            viewModel.sendIntent(InquiriesUiIntent.Sync)
            advanceUntilIdle()

            assertEquals(InquiryListContent.Items(listOf(inquirySummary(1))), viewModel.state.value.content)
        }

    @Test
    fun `문의를 누르면 그 문의 상세로 간다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(InquiriesUiIntent.InquiryClicked(7))
            advanceUntilIdle()

            assertEquals(listOf<Page>(InquiryDetailPage(7)), navigationHelper.navigatedTo)
        }

    private fun createViewModel() =
        InquiriesViewModel(
            getMyInquiriesUseCase = GetMyInquiriesUseCase(repository, NoOpInquiryMessageHelper),
            navigationHelper = navigationHelper,
        )
}
