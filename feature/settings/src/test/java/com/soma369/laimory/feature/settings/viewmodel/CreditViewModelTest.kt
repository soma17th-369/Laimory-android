package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.credit.CreditCosts
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.CreditRepository
import com.soma369.laimory.core.domain.usecase.credit.GetTimelineCreditUseCase
import com.soma369.laimory.feature.settings.credit.CreditBalanceStore
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCreditRepository()
    private val store = CreditBalanceStore()
    private val navigationHelper = RecordingNavigationHelper()

    @Test
    fun `들어오면 잔액과 1개당 비용을 함께 보여 준다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.remaining = 42
            val viewModel = createViewModel()

            viewModel.sendIntent(CreditUiIntent.Refresh)
            advanceUntilIdle()

            assertEquals(42, viewModel.state.value.remaining)
            assertEquals(1, viewModel.state.value.cost)
            assertFalse(viewModel.state.value.isLoading)
        }

    @Test
    fun `기억해 둔 잔액이 있으면 다시 받는 동안에도 그 값을 보인다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 설정이 받아 둔 값. 들어올 때마다 비우면 로딩이 번쩍인다.
            store.updateRemaining(store.beginRequest(), 42)
            repository.gate = CompletableDeferred()
            val viewModel = createViewModel()

            viewModel.sendIntent(CreditUiIntent.Refresh)
            runCurrent()

            assertEquals(42, viewModel.state.value.remaining)
            assertTrue(viewModel.state.value.isLoading)
        }

    @Test
    fun `잔액이나 비용을 못 받으면 0 이 아니라 받지 못함으로 둔다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 크레딧 행이 없으면 서버가 500 을 낸다. 0 으로 보이면 남은 게 없는 줄 안다.
            store.updateRemaining(store.beginRequest(), 42)
            repository.failure = ApiException.ServerException(rawCode = 500)
            val viewModel = createViewModel()

            viewModel.sendIntent(CreditUiIntent.Refresh)
            advanceUntilIdle()

            assertNull(viewModel.state.value.remaining)
            assertFalse(viewModel.state.value.isLoading)
        }

    @Test
    fun `계정 경계에서 비운 뒤 도착한 이전 계정의 응답은 버린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val late = CompletableDeferred<Unit>()
            repository.gate = late
            repository.remaining = 42
            val viewModel = createViewModel()
            viewModel.sendIntent(CreditUiIntent.Refresh)
            runCurrent()

            store.clear()
            late.complete(Unit)
            advanceUntilIdle()

            assertNull(viewModel.state.value.remaining)
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

            assertEquals(41, viewModel.state.value.remaining)
        }

    @Test
    fun `상품을 누르면 충전은 준비 중이라고 알린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(CreditUiIntent.PackageClicked(amount = 7))

            assertEquals(CreditUiSideEffect.ShowSnackbar("크레딧 충전은 준비 중이에요"), viewModel.sideEffect.first())
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
                balanceStore = store,
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
