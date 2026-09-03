package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.push.PushSettings
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.PushSettingsRepository
import com.soma369.laimory.core.domain.usecase.push.GetPushSettingsUseCase
import com.soma369.laimory.core.domain.usecase.push.UpdateDailyReminderEnabledUseCase
import com.soma369.laimory.core.domain.usecase.push.UpdatePushEnabledUseCase
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiContent
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiIntent
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiSideEffect
import com.soma369.laimory.feature.settings.state.NotificationToggle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePushSettingsRepository()
    private val navigationHelper = RecordingNavigationHelper()

    @Test
    fun `조회 성공은 서버 값을 그대로 그린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)

            val viewModel = createLoadedViewModel()

            assertEquals(
                PushSettings(isPushEnabled = true, isDailyReminderEnabled = false),
                viewModel.state.value.settings,
            )
        }

    @Test
    fun `조회 실패는 기본값 대신 실패 상태를 두고 재시도로 복구한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.getFailure = ApiException.NetworkException()

            val viewModel = createLoadedViewModel()

            assertEquals(NotificationSettingsUiContent.LoadFailed, viewModel.state.value.content)
            assertEquals(null, viewModel.state.value.settings)

            repository.getFailure = null
            viewModel.sendIntent(NotificationSettingsUiIntent.RetryLoad)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.content is NotificationSettingsUiContent.Settings)
        }

    @Test
    fun `표시값은 서버가 받아들인 뒤에만 바뀐다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()
            val gate = CompletableDeferred<Unit>()
            repository.updateGate = gate

            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.DAILY_REMINDER, true),
            )
            runCurrent()

            // 응답 전에는 이전 값 그대로이고 그 줄만 잠긴다.
            assertFalse(viewModel.state.value.settings!!.isDailyReminderEnabled)
            assertTrue(viewModel.state.value.isUpdating(NotificationToggle.DAILY_REMINDER))
            assertFalse(viewModel.state.value.isUpdating(NotificationToggle.PUSH))

            gate.complete(Unit)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.settings!!.isDailyReminderEnabled)
            assertFalse(viewModel.state.value.isUpdating(NotificationToggle.DAILY_REMINDER))
            assertEquals(listOf(true), repository.dailyReminderUpdates)
        }

    @Test
    fun `변경에 실패하면 이전 값을 지키고 안내한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = true)
            val viewModel = createLoadedViewModel()
            repository.updateFailure = ApiException.NetworkException()

            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.PUSH, false),
            )
            advanceUntilIdle()

            assertTrue(viewModel.state.value.settings!!.isPushEnabled)
            assertEquals(
                NotificationSettingsUiSideEffect.ShowSnackbar("네트워크 상태를 확인한 뒤 다시 시도해주세요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `전체 알림이 꺼져 있으면 일일 리마인더 요청을 받지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = false, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.DAILY_REMINDER, true),
            )
            advanceUntilIdle()

            assertTrue(repository.dailyReminderUpdates.isEmpty())
            assertFalse(viewModel.state.value.settings!!.isDailyReminderEnabled)
        }

    @Test
    fun `이미 같은 값이면 서버를 부르지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.PUSH, true),
            )
            advanceUntilIdle()

            assertTrue(repository.pushUpdates.isEmpty())
        }

    @Test
    fun `응답을 기다리는 동안 같은 줄을 다시 눌러도 한 번만 보낸다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()
            val gate = CompletableDeferred<Unit>()
            repository.updateGate = gate

            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.PUSH, false),
            )
            runCurrent()
            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.PUSH, false),
            )
            runCurrent()
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf(false), repository.pushUpdates)
        }

    private fun TestScope.createLoadedViewModel(): NotificationSettingsViewModel {
        val viewModel =
            NotificationSettingsViewModel(
                getPushSettingsUseCase = GetPushSettingsUseCase(repository, NoOpMessageHelper),
                updatePushEnabledUseCase = UpdatePushEnabledUseCase(repository, NoOpMessageHelper),
                updateDailyReminderEnabledUseCase =
                    UpdateDailyReminderEnabledUseCase(repository, NoOpMessageHelper),
                navigationHelper = navigationHelper,
            )
        viewModel.sendIntent(NotificationSettingsUiIntent.Initialize)
        advanceUntilIdle()
        return viewModel
    }

    private class FakePushSettingsRepository : PushSettingsRepository {
        var settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
        var getFailure: ApiException? = null
        var updateFailure: ApiException? = null
        var updateGate: CompletableDeferred<Unit>? = null
        val pushUpdates = mutableListOf<Boolean>()
        val dailyReminderUpdates = mutableListOf<Boolean>()

        override suspend fun getPushSettings(): PushSettings {
            getFailure?.let { throw it }
            return settings
        }

        override suspend fun updatePushEnabled(isEnabled: Boolean) {
            pushUpdates += isEnabled
            awaitGate()
            updateFailure?.let { throw it }
        }

        override suspend fun updateDailyReminderEnabled(isEnabled: Boolean) {
            dailyReminderUpdates += isEnabled
            awaitGate()
            updateFailure?.let { throw it }
        }

        private suspend fun awaitGate() {
            updateGate?.let { gate ->
                updateGate = null
                gate.await()
            }
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

    private object NoOpMessageHelper : MessageHelper {
        override fun send(message: UserMessage) = Unit
    }
}
