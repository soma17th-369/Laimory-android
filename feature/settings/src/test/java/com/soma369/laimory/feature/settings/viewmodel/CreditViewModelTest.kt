package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.credit.CreditCosts
import com.soma369.laimory.core.domain.model.credit.TimelineCredit
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.CreditRepository
import com.soma369.laimory.core.domain.usecase.credit.GetTimelineCreditUseCase
import com.soma369.laimory.feature.settings.state.CreditBalance
import com.soma369.laimory.feature.settings.state.CreditUiIntent
import com.soma369.laimory.feature.settings.state.CreditUiSideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCreditRepository()
    private val navigationHelper = RecordingNavigationHelper()

    @Test
    fun `들어오면 잔액과 1개당 비용을 함께 보여 준다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.remaining = 42
            val viewModel = createViewModel()

            viewModel.sendIntent(CreditUiIntent.Refresh)
            advanceUntilIdle()

            assertEquals(CreditBalance.Loaded(TimelineCredit(cost = 1, remaining = 42)), viewModel.state.value.balance)
        }

    @Test
    fun `잔액이나 비용을 못 받으면 0 이 아니라 받지 못함으로 둔다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 크레딧 행이 없으면 서버가 500 을 낸다. 0 으로 보이면 남은 게 없는 줄 안다.
            repository.failure = ApiException.ServerException(rawCode = 500)
            val viewModel = createViewModel()

            viewModel.sendIntent(CreditUiIntent.Refresh)
            advanceUntilIdle()

            assertEquals(CreditBalance.Unavailable, viewModel.state.value.balance)
        }

    @Test
    fun `다시 들어오면 받는 중으로 돌아가 옛 잔액을 남기지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.sendIntent(CreditUiIntent.Refresh)
            advanceUntilIdle()

            // Activity 수명 ViewModel 이라 다른 계정으로 다시 들어와도 같은 인스턴스다.
            repository.gate = CompletableDeferred()
            viewModel.sendIntent(CreditUiIntent.Refresh)
            runCurrent()

            assertEquals(CreditBalance.Loading, viewModel.state.value.balance)
        }

    @Test
    fun `늦게 끝난 이전 조회가 새 결과를 덮지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            val slow = CompletableDeferred<Unit>()
            repository.gate = slow
            repository.remaining = 42
            viewModel.sendIntent(CreditUiIntent.Refresh)
            runCurrent()

            repository.gate = null
            repository.remaining = 41
            viewModel.sendIntent(CreditUiIntent.Refresh)
            runCurrent()
            slow.complete(Unit)
            advanceUntilIdle()

            assertEquals(CreditBalance.Loaded(TimelineCredit(cost = 1, remaining = 41)), viewModel.state.value.balance)
        }

    @Test
    fun `상품을 누르면 충전은 곧 열린다고 알린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(CreditUiIntent.PackageClicked(amount = 7))

            assertEquals(CreditUiSideEffect.ShowSnackbar("크레딧 충전은 곧 열려요"), viewModel.sideEffect.first())
        }

    @Test
    fun `뒤로 가기를 누르면 이전 화면으로 돌아간다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(CreditUiIntent.NavigateBack)
            advanceUntilIdle()

            assertEquals(1, navigationHelper.backCount)
        }

    private fun TestScope.createViewModel(): CreditViewModel {
        val viewModel =
            CreditViewModel(
                getTimelineCredit = GetTimelineCreditUseCase(repository),
                navigationHelper = navigationHelper,
            )
        advanceUntilIdle()
        return viewModel
    }

    private class FakeCreditRepository : CreditRepository {
        var remaining = 60
        var failure: Throwable? = null

        /** 채우면 완료될 때까지 응답하지 않는다. 응답이 늦게 오는 경우를 만든다. 호출 시점의 값을 붙잡는다. */
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun getRemainingCredits(): Int {
            val pending = gate
            val value = remaining
            pending?.await()
            failure?.let { throw it }
            return value
        }

        override suspend fun getCosts(): CreditCosts = CreditCosts(timelineCreation = 1)
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
