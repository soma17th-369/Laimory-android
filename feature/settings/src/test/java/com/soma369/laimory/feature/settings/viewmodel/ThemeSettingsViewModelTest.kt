package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.settings.AppThemeMode
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.AppThemeRepository
import com.soma369.laimory.core.domain.usecase.settings.ObserveAppThemeModeUseCase
import com.soma369.laimory.core.domain.usecase.settings.SetAppThemeModeUseCase
import com.soma369.laimory.feature.settings.state.ThemeSettingsUiIntent
import com.soma369.laimory.feature.settings.state.ThemeSettingsUiSideEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAppThemeRepository()
    private val navigationHelper = RecordingNavigationHelper()

    @Test
    fun `저장된 값을 그대로 보여 준다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.mode.value = AppThemeMode.DARK

            val viewModel = createViewModel()

            assertEquals(AppThemeMode.DARK, viewModel.state.value.selected)
        }

    @Test
    fun `고르면 저장하고 표시가 따라온다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(ThemeSettingsUiIntent.Select(AppThemeMode.LIGHT))
            advanceUntilIdle()

            assertEquals(listOf(AppThemeMode.LIGHT), repository.saved)
            assertEquals(AppThemeMode.LIGHT, viewModel.state.value.selected)
        }

    @Test
    fun `이미 고른 값을 다시 눌러도 저장하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.mode.value = AppThemeMode.LIGHT
            val viewModel = createViewModel()

            viewModel.sendIntent(ThemeSettingsUiIntent.Select(AppThemeMode.LIGHT))
            advanceUntilIdle()

            assertTrue(repository.saved.isEmpty())
        }

    @Test
    fun `저장에 실패하면 표시가 그대로 남고 안내한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            repository.failure = IllegalStateException("disk full")

            viewModel.sendIntent(ThemeSettingsUiIntent.Select(AppThemeMode.DARK))
            advanceUntilIdle()

            // 표시값은 저장소 흐름만 보므로 실패하면 저절로 이전 자리에 남는다.
            assertEquals(AppThemeMode.SYSTEM, viewModel.state.value.selected)
            assertEquals(
                ThemeSettingsUiSideEffect.ShowSnackbar("설정을 저장하지 못했어요. 잠시 후 다시 시도해주세요."),
                viewModel.sideEffect.first(),
            )
        }

    private fun TestScope.createViewModel(): ThemeSettingsViewModel {
        val viewModel =
            ThemeSettingsViewModel(
                observeAppThemeModeUseCase = ObserveAppThemeModeUseCase(repository),
                setAppThemeModeUseCase = SetAppThemeModeUseCase(repository),
                navigationHelper = navigationHelper,
            )
        advanceUntilIdle()
        return viewModel
    }

    private class FakeAppThemeRepository : AppThemeRepository {
        val mode = MutableStateFlow(AppThemeMode.SYSTEM)
        val saved = mutableListOf<AppThemeMode>()
        var failure: Throwable? = null

        override val themeMode: Flow<AppThemeMode> = mode

        override suspend fun setThemeMode(mode: AppThemeMode) {
            failure?.let { throw it }
            saved += mode
            this.mode.value = mode
        }
    }

    private class RecordingNavigationHelper : NavigationHelper {
        override fun navigateTo(page: Page) = Unit

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() = Unit
    }
}
