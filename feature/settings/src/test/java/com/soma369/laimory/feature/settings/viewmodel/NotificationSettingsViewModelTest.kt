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
    fun `화면은 누른 즉시 바뀌고 서버에는 손이 멈춘 뒤 보낸다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.DAILY_REMINDER, true),
            )
            runCurrent()

            // 응답을 기다리지 않는다. 기다리면 누른 자리가 움직이지 않아 눌리지 않은 것처럼 보인다.
            assertTrue(viewModel.state.value.settings!!.isDailyReminderEnabled)
            assertTrue(repository.dailyReminderUpdates.isEmpty())

            advanceUntilIdle()

            assertEquals(listOf(true), repository.dailyReminderUpdates)
            assertTrue(viewModel.state.value.confirmedSettings!!.isDailyReminderEnabled)
        }

    @Test
    fun `변경에 실패하면 서버 값으로 되돌리고 안내한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = true)
            val viewModel = createLoadedViewModel()
            repository.updateFailure = ApiException.NetworkException()

            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.PUSH, false),
            )
            runCurrent()

            assertFalse(viewModel.state.value.settings!!.isPushEnabled)

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
    fun `연타해도 마지막 값 하나만 보낸다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()

            // 켰다 껐다 켠다. 중간 값까지 보내면 서버가 그 순서를 그대로 겪는다.
            listOf(true, false, true).forEach { isEnabled ->
                viewModel.sendIntent(
                    NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.DAILY_REMINDER, isEnabled),
                )
                runCurrent()
            }
            advanceUntilIdle()

            assertEquals(listOf(true), repository.dailyReminderUpdates)
            assertTrue(viewModel.state.value.settings!!.isDailyReminderEnabled)
        }

    @Test
    fun `눌렀다 제자리로 되돌리면 서버를 부르지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()

            listOf(true, false).forEach { isEnabled ->
                viewModel.sendIntent(
                    NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.DAILY_REMINDER, isEnabled),
                )
                runCurrent()
            }
            advanceUntilIdle()

            assertTrue(repository.dailyReminderUpdates.isEmpty())
            assertFalse(viewModel.state.value.settings!!.isDailyReminderEnabled)
        }

    @Test
    fun `전체 알림을 끄면 아직 보내지 않은 일일 리마인더 변경을 버린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.DAILY_REMINDER, true),
            )
            runCurrent()
            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.PUSH, false),
            )
            advanceUntilIdle()

            // 전체를 끈 뒤에는 사용자가 볼 수 없는 값이다. 서버에 남기지 않는다.
            assertTrue(repository.dailyReminderUpdates.isEmpty())
            assertFalse(viewModel.state.value.settings!!.isDailyReminderEnabled)
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
        val pushUpdates = mutableListOf<Boolean>()
        val dailyReminderUpdates = mutableListOf<Boolean>()

        override suspend fun getPushSettings(): PushSettings {
            getFailure?.let { throw it }
            return settings
        }

        override suspend fun updatePushEnabled(isEnabled: Boolean) {
            pushUpdates += isEnabled
            updateFailure?.let { throw it }
        }

        override suspend fun updateDailyReminderEnabled(isEnabled: Boolean) {
            dailyReminderUpdates += isEnabled
            updateFailure?.let { throw it }
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
