package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.InquiryNotFoundException
import com.soma369.laimory.core.domain.usecase.inquiry.GetInquiryDetailUseCase
import com.soma369.laimory.feature.settings.state.InquiryDetailContent
import com.soma369.laimory.feature.settings.state.InquiryDetailUiIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InquiryDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeInquiryHistoryRepository()
    private val navigationHelper = RecordingInquiryNavigationHelper()

    @Test
    fun `문의를 열면 보낸 내용을 보여 준다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.details[1] = Result.success(inquiryDetail(1))
            val viewModel = createViewModel()

            viewModel.sendIntent(InquiryDetailUiIntent.Load(1))
            advanceUntilIdle()

            assertEquals(InquiryDetailContent.Loaded(inquiryDetail(1)), viewModel.state.value.content)
        }

    @Test
    fun `다른 문의를 열면 이전 문의 내용을 먼저 지운다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // ViewModel 이 Activity 수명이라 같은 인스턴스가 온다. 잠깐이라도 다른 문의가 보이면 안 된다.
            repository.details[1] = Result.success(inquiryDetail(1))
            repository.details[2] = Result.success(inquiryDetail(2))
            repository.gates[2] = CompletableDeferred()
            val viewModel = createViewModel()
            viewModel.sendIntent(InquiryDetailUiIntent.Load(1))
            advanceUntilIdle()

            viewModel.sendIntent(InquiryDetailUiIntent.Load(2))
            advanceUntilIdle()
            assertEquals(InquiryDetailContent.Loading, viewModel.state.value.content)

            repository.gates.getValue(2).complete(Unit)
            advanceUntilIdle()
            assertEquals(InquiryDetailContent.Loaded(inquiryDetail(2)), viewModel.state.value.content)
        }

    @Test
    fun `늦게 온 이전 문의 결과가 새 문의 자리에 들어오지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.details[1] = Result.success(inquiryDetail(1))
            repository.details[2] = Result.success(inquiryDetail(2))
            repository.gates[1] = CompletableDeferred()
            val viewModel = createViewModel()

            viewModel.sendIntent(InquiryDetailUiIntent.Load(1))
            advanceUntilIdle()
            viewModel.sendIntent(InquiryDetailUiIntent.Load(2))
            advanceUntilIdle()
            repository.gates.getValue(1).complete(Unit)
            advanceUntilIdle()

            assertEquals(2L, viewModel.state.value.inquiryId)
            assertEquals(InquiryDetailContent.Loaded(inquiryDetail(2)), viewModel.state.value.content)
        }

    @Test
    fun `없거나 남의 문의면 찾을 수 없다고 보여 준다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.details[9] =
                Result.failure(InquiryNotFoundException(ApiException.ClientException(errorCode = -404, rawCode = 404)))
            val viewModel = createViewModel()

            viewModel.sendIntent(InquiryDetailUiIntent.Load(9))
            advanceUntilIdle()

            assertEquals(InquiryDetailContent.NotFound, viewModel.state.value.content)
        }

    @Test
    fun `경로 인자가 깨졌으면 찾을 수 없는 문의로 보여 준다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(InquiryDetailUiIntent.Load(null))
            advanceUntilIdle()

            assertEquals(InquiryDetailContent.NotFound, viewModel.state.value.content)
        }

    @Test
    fun `받기에 실패하면 다시 시도할 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.details[1] = Result.failure(ApiException.NetworkException())
            val viewModel = createViewModel()
            viewModel.sendIntent(InquiryDetailUiIntent.Load(1))
            advanceUntilIdle()
            assertEquals(InquiryDetailContent.LoadFailed, viewModel.state.value.content)

            repository.details[1] = Result.success(inquiryDetail(1))
            viewModel.sendIntent(InquiryDetailUiIntent.Retry)
            advanceUntilIdle()

            assertEquals(InquiryDetailContent.Loaded(inquiryDetail(1)), viewModel.state.value.content)
        }

    private fun createViewModel() =
        InquiryDetailViewModel(
            getInquiryDetailUseCase = GetInquiryDetailUseCase(repository, NoOpInquiryMessageHelper),
            navigationHelper = navigationHelper,
        )
}
