package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.push.PushSettings
import com.soma369.laimory.core.domain.model.reminder.DailyReminderRecordDay
import com.soma369.laimory.core.domain.model.reminder.DailyReminderSettings
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.PushSettingsRepository
import com.soma369.laimory.core.domain.usecase.push.GetPushSettingsUseCase
import com.soma369.laimory.core.domain.usecase.push.UpdatePushEnabledUseCase
import com.soma369.laimory.core.domain.usecase.reminder.IsDailyReminderDeliveredTodayUseCase
import com.soma369.laimory.core.domain.usecase.reminder.ObserveDailyReminderSettingsUseCase
import com.soma369.laimory.core.domain.usecase.reminder.SetDailyReminderEnabledUseCase
import com.soma369.laimory.core.domain.usecase.reminder.SetDailyReminderTimeUseCase
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiContent
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiIntent
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiSideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePushSettingsRepository()
    private val reminderRepository = FakeDailyReminderRepository()
    private val navigationHelper = RecordingNavigationHelper()

    @Test
    fun `조회 성공은 서버의 전체 알림 값을 그대로 그린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = false, isDailyReminderEnabled = true)

            val viewModel = createLoadedViewModel()

            assertEquals(false, viewModel.state.value.isPushEnabled)
            assertEquals(false, viewModel.state.value.confirmedPushEnabled)
        }

    @Test
    fun `일일 리마인더는 서버 값이 아니라 기기 값을 그린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 서버 값은 앱이 꺼 두기만 한다. 화면이 그것을 읽으면 모두 꺼진 것으로 보인다.
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            reminderRepository.setEnabled(true)

            val viewModel = createLoadedViewModel()

            assertTrue(viewModel.state.value.reminder.isEnabled)
        }

    @Test
    fun `조회 실패는 기본값 대신 실패 상태를 두고 재시도로 복구한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.getFailure = ApiException.NetworkException()

            val viewModel = createLoadedViewModel()

            assertEquals(NotificationSettingsUiContent.LoadFailed, viewModel.state.value.content)
            assertEquals(null, viewModel.state.value.isPushEnabled)

            repository.getFailure = null
            viewModel.sendIntent(NotificationSettingsUiIntent.RetryLoad)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.content is NotificationSettingsUiContent.Settings)
        }

    @Test
    fun `전체 알림은 누른 즉시 바뀌고 서버에는 손이 멈춘 뒤 보낸다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = false, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(NotificationSettingsUiIntent.PushToggled(true))
            runCurrent()

            // 응답을 기다리지 않는다. 기다리면 누른 자리가 움직이지 않아 눌리지 않은 것처럼 보인다.
            assertEquals(true, viewModel.state.value.isPushEnabled)
            assertTrue(repository.pushUpdates.isEmpty())

            advanceUntilIdle()

            assertEquals(listOf(true), repository.pushUpdates)
            assertEquals(true, viewModel.state.value.confirmedPushEnabled)
            // 기기 리마인더가 띄울지 판정할 거울에도 남는다.
            assertEquals(true, reminderRepository.mirrorValue)
        }

    @Test
    fun `변경에 실패하면 서버 값으로 되돌리고 안내한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()
            repository.updateFailure = ApiException.NetworkException()

            viewModel.sendIntent(NotificationSettingsUiIntent.PushToggled(false))
            runCurrent()

            assertEquals(false, viewModel.state.value.isPushEnabled)

            advanceUntilIdle()

            assertEquals(true, viewModel.state.value.isPushEnabled)
            assertEquals(
                NotificationSettingsUiSideEffect.ShowSnackbar("네트워크 상태를 확인한 뒤 다시 시도해주세요."),
                viewModel.sideEffect.first(),
            )
            // 서버가 거절한 값을 거울에 남기지 않는다.
            assertEquals(true, reminderRepository.mirrorValue)
        }

    @Test
    fun `이미 같은 값이면 서버를 부르지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(NotificationSettingsUiIntent.PushToggled(true))
            advanceUntilIdle()

            assertTrue(repository.pushUpdates.isEmpty())
        }

    @Test
    fun `연타해도 마지막 값 하나만 보낸다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = false, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()

            // 켰다 껐다 켠다. 중간 값까지 보내면 서버가 그 순서를 그대로 겪는다.
            listOf(true, false, true).forEach { isEnabled ->
                viewModel.sendIntent(NotificationSettingsUiIntent.PushToggled(isEnabled))
                runCurrent()
            }
            advanceUntilIdle()

            assertEquals(listOf(true), repository.pushUpdates)
            assertEquals(true, viewModel.state.value.isPushEnabled)
        }

    @Test
    fun `눌렀다 제자리로 되돌리면 서버를 부르지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = false, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()

            listOf(true, false).forEach { isEnabled ->
                viewModel.sendIntent(NotificationSettingsUiIntent.PushToggled(isEnabled))
                runCurrent()
            }
            advanceUntilIdle()

            assertTrue(repository.pushUpdates.isEmpty())
            assertEquals(false, viewModel.state.value.isPushEnabled)
        }

    @Test
    fun `보내는 중에 되돌리면 앞 요청을 끊지 않고 뒤이어 다시 보낸다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 끊으면 그 요청이 서버에 닿았는지 모르는 채 남는다. 그사이 화면이 원래 값으로 돌아와
            // 있으면 보낼 것이 없다고 판단해, 서버만 켜진 채 화면과 갈라진다.
            repository.settings = PushSettings(isPushEnabled = false, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()
            val gate = CompletableDeferred<Unit>()
            repository.updateGate = gate

            viewModel.sendIntent(NotificationSettingsUiIntent.PushToggled(true))
            advanceTimeBy(DEBOUNCE_MARGIN_MILLIS)
            runCurrent()
            assertEquals(listOf(true), repository.pushUpdates)

            // 응답이 오기 전에 되돌린다.
            viewModel.sendIntent(NotificationSettingsUiIntent.PushToggled(false))
            advanceTimeBy(DEBOUNCE_MARGIN_MILLIS)
            runCurrent()
            // 앞 요청이 끝나기 전에는 줄을 서 있는다.
            assertEquals(listOf(true), repository.pushUpdates)

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf(true, false), repository.pushUpdates)
            assertEquals(false, viewModel.state.value.isPushEnabled)
            assertEquals(false, viewModel.state.value.confirmedPushEnabled)
        }

    @Test
    fun `보내는 사이 다시 누른 값은 앞 요청이 실패해도 되돌리지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = false, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()
            val gate = CompletableDeferred<Unit>()
            repository.updateGate = gate
            repository.updateFailure = ApiException.NetworkException()

            viewModel.sendIntent(NotificationSettingsUiIntent.PushToggled(true))
            advanceTimeBy(DEBOUNCE_MARGIN_MILLIS)
            runCurrent()
            viewModel.sendIntent(NotificationSettingsUiIntent.PushToggled(false))
            gate.complete(Unit)
            advanceUntilIdle()

            // 실패한 값은 사용자가 이미 지나온 값이다. 되돌리면 방금 한 조작을 뒤엎는다.
            assertEquals(false, viewModel.state.value.isPushEnabled)
            // 되돌린 값이 서버 값과 같아져 보낼 것이 없다.
            assertEquals(listOf(true), repository.pushUpdates)
        }

    @Test
    fun `화면에 다시 들어오면 서버에 다시 묻는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 이 ViewModel 은 Activity 에 매여 있어 로그아웃 뒤 다른 계정으로 들어와도 살아 있다.
            // 들고 있던 값을 그대로 쓰면 이전 계정의 설정을 지금 계정의 값으로 보여 준다.
            repository.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()

            repository.settings = PushSettings(isPushEnabled = false, isDailyReminderEnabled = false)
            viewModel.sendIntent(NotificationSettingsUiIntent.Initialize)
            advanceUntilIdle()

            assertEquals(false, viewModel.state.value.isPushEnabled)
        }

    @Test
    fun `일일 리마인더는 서버에 보내지 않고 기기에 바로 저장한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(NotificationSettingsUiIntent.DailyReminderToggled(false))
            advanceUntilIdle()

            assertFalse(reminderRepository.currentSettings.isEnabled)
            assertFalse(viewModel.state.value.reminder.isEnabled)
            assertTrue(repository.dailyReminderUpdates.isEmpty())
        }

    @Test
    fun `전체 알림이 꺼져 있으면 일일 리마인더 요청을 받지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.settings = PushSettings(isPushEnabled = false, isDailyReminderEnabled = false)
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(NotificationSettingsUiIntent.DailyReminderToggled(false))
            viewModel.sendIntent(NotificationSettingsUiIntent.OpenReminderTimePicker)
            advanceUntilIdle()

            assertTrue(reminderRepository.currentSettings.isEnabled)
            assertEquals(null, viewModel.state.value.reminderTimeDraft)
        }

    @Test
    fun `전체 알림을 꺼도 일일 리마인더 값은 그대로 남는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 잠기는 것은 줄이지 값이 아니다. 다시 켰을 때 종전 선택이 그대로 살아 있어야 한다.
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(NotificationSettingsUiIntent.PushToggled(false))
            advanceUntilIdle()

            assertTrue(viewModel.state.value.reminder.isEnabled)
        }

    @Test
    fun `시각은 시트에서 굴리는 동안 저장하지 않고 확인에서 저장한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(NotificationSettingsUiIntent.OpenReminderTimePicker)
            advanceUntilIdle()
            assertEquals(DailyReminderSettings.DEFAULT_TIME, viewModel.state.value.reminderTimeDraft)

            viewModel.sendIntent(NotificationSettingsUiIntent.ReminderTimeChanged(LocalTime.of(22, 0)))
            advanceUntilIdle()
            assertEquals(DailyReminderSettings.DEFAULT_TIME, reminderRepository.currentSettings.time)

            viewModel.sendIntent(NotificationSettingsUiIntent.ConfirmReminderTime)
            advanceUntilIdle()

            assertEquals(LocalTime.of(22, 0), reminderRepository.currentSettings.time)
            assertEquals(LocalTime.of(22, 0), viewModel.state.value.reminder.time)
            assertEquals(null, viewModel.state.value.reminderTimeDraft)
        }

    @Test
    fun `오늘 이미 받은 날 시간대를 바꾸면 내일부터라고 알린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            reminderRepository.setLastDeliveredRecordDate(DailyReminderRecordDay.of(LocalDateTime.now()))
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(NotificationSettingsUiIntent.OpenReminderTimePicker)
            viewModel.sendIntent(NotificationSettingsUiIntent.ReminderTimeChanged(LocalTime.of(22, 0)))
            viewModel.sendIntent(NotificationSettingsUiIntent.ConfirmReminderTime)
            advanceUntilIdle()

            assertEquals(LocalTime.of(22, 0), reminderRepository.currentSettings.time)
            assertEquals(
                NotificationSettingsUiSideEffect.ShowSnackbar("오늘 알림은 이미 보냈어요. 내일부터 22시에 알려드릴게요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `오늘 아직 안 받았으면 시간대를 바꿔도 알리지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 어제 받은 것은 오늘 받은 것이 아니다. 오늘 몫은 예약이 따라잡아 곧 온다.
            reminderRepository.setLastDeliveredRecordDate(DailyReminderRecordDay.of(LocalDateTime.now()).minusDays(1))
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(NotificationSettingsUiIntent.OpenReminderTimePicker)
            viewModel.sendIntent(NotificationSettingsUiIntent.ReminderTimeChanged(LocalTime.of(22, 0)))
            viewModel.sendIntent(NotificationSettingsUiIntent.ConfirmReminderTime)
            advanceUntilIdle()

            assertEquals(LocalTime.of(22, 0), reminderRepository.currentSettings.time)
            assertNull(withTimeoutOrNull(1) { viewModel.sideEffect.first() })
        }

    @Test
    fun `정시가 아닌 값은 정시로 내려 시트를 연다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 정시 피커가 생기기 전에 저장된 값. 시트는 정시만 고른다.
            reminderRepository.setTime(LocalTime.of(21, 5))
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(NotificationSettingsUiIntent.OpenReminderTimePicker)
            advanceUntilIdle()

            assertEquals(LocalTime.of(21, 0), viewModel.state.value.reminderTimeDraft)
        }

    @Test
    fun `시트를 닫으면 굴린 시각은 버린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(NotificationSettingsUiIntent.OpenReminderTimePicker)
            viewModel.sendIntent(NotificationSettingsUiIntent.ReminderTimeChanged(LocalTime.of(7, 0)))
            viewModel.sendIntent(NotificationSettingsUiIntent.DismissReminderTimePicker)
            advanceUntilIdle()

            assertEquals(DailyReminderSettings.DEFAULT_TIME, reminderRepository.currentSettings.time)
            assertEquals(null, viewModel.state.value.reminderTimeDraft)
        }

    @Test
    fun `리마인더가 꺼져 있으면 시각 시트를 열지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            reminderRepository.setEnabled(false)
            val viewModel = createLoadedViewModel()

            viewModel.sendIntent(NotificationSettingsUiIntent.OpenReminderTimePicker)
            advanceUntilIdle()

            assertEquals(null, viewModel.state.value.reminderTimeDraft)
        }

    private fun TestScope.createLoadedViewModel(): NotificationSettingsViewModel {
        val viewModel =
            NotificationSettingsViewModel(
                getPushSettingsUseCase = GetPushSettingsUseCase(repository, reminderRepository, NoOpMessageHelper),
                updatePushEnabledUseCase = UpdatePushEnabledUseCase(repository, reminderRepository, NoOpMessageHelper),
                observeDailyReminderSettingsUseCase = ObserveDailyReminderSettingsUseCase(reminderRepository),
                setDailyReminderEnabledUseCase = SetDailyReminderEnabledUseCase(reminderRepository),
                setDailyReminderTimeUseCase = SetDailyReminderTimeUseCase(reminderRepository),
                isDailyReminderDeliveredTodayUseCase = IsDailyReminderDeliveredTodayUseCase(reminderRepository),
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
