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
import kotlinx.coroutines.test.advanceTimeBy
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

    @Test
    fun `보내는 중에 되돌리면 앞 요청을 끊지 않고 뒤이어 다시 보낸다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 끊으면 그 요청이 서버에 닿았는지 모르는 채 남는다. 그사이 화면이 원래 값으로 돌아와
            // 있으면 보낼 것이 없다고 판단해, 서버만 켜진 채 화면과 갈라진다.
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()
            val gate = CompletableDeferred<Unit>()
            repository.updateGate = gate

            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.DAILY_REMINDER, true),
            )
            advanceTimeBy(DEBOUNCE_MARGIN_MILLIS)
            runCurrent()
            assertEquals(listOf(true), repository.dailyReminderUpdates)

            // 응답이 오기 전에 되돌린다.
            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.DAILY_REMINDER, false),
            )
            advanceTimeBy(DEBOUNCE_MARGIN_MILLIS)
            runCurrent()
            // 앞 요청이 끝나기 전에는 줄을 서 있는다.
            assertEquals(listOf(true), repository.dailyReminderUpdates)

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf(true, false), repository.dailyReminderUpdates)
            assertFalse(viewModel.state.value.settings!!.isDailyReminderEnabled)
            assertFalse(viewModel.state.value.confirmedSettings!!.isDailyReminderEnabled)
        }

    @Test
    fun `보내는 사이 다시 누른 값은 앞 요청이 실패해도 되돌리지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()
            val gate = CompletableDeferred<Unit>()
            repository.updateGate = gate
            repository.updateFailure = ApiException.NetworkException()

            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.DAILY_REMINDER, true),
            )
            advanceTimeBy(DEBOUNCE_MARGIN_MILLIS)
            runCurrent()
            viewModel.sendIntent(
                NotificationSettingsUiIntent.ToggleChanged(NotificationToggle.DAILY_REMINDER, false),
            )
            gate.complete(Unit)
            advanceUntilIdle()

            // 실패한 값은 사용자가 이미 지나온 값이다. 되돌리면 방금 한 조작을 뒤엎는다.
            assertFalse(viewModel.state.value.settings!!.isDailyReminderEnabled)
            // 되돌린 값이 서버 값과 같아져 보낼 것이 없다.
            assertEquals(listOf(true), repository.dailyReminderUpdates)
        }

    @Test
    fun `화면에 다시 들어오면 서버에 다시 묻는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 이 ViewModel 은 Activity 에 매여 있어 로그아웃 뒤 다른 계정으로 들어와도 살아 있다.
            // 들고 있던 값을 그대로 쓰면 이전 계정의 설정을 지금 계정의 값으로 보여 준다.
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()

            repository.settings = PushSettings(isPushEnabled = false, isDailyReminderEnabled = true)
            viewModel.sendIntent(NotificationSettingsUiIntent.Initialize)
            advanceUntilIdle()

            assertEquals(
                PushSettings(isPushEnabled = false, isDailyReminderEnabled = true),
                viewModel.state.value.settings,
            )
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

    private companion object {
        /** 디바운스(400ms)를 확실히 넘기는 시간. */
        const val DEBOUNCE_MARGIN_MILLIS = 1_000L
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

        /** 첫 변경 요청 하나만 붙잡는다. 보내는 중에 일어나는 일을 시험할 때 쓴다. */
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
