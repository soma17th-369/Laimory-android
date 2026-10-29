package com.soma369.laimory.reminder

import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import com.soma369.laimory.core.domain.usecase.reminder.HandOffDailyReminderUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyReminderDeliveryTest {
    @Test
    fun `같은 시각의 알람 둘이 함께 와도 한 번만 띄운다`() =
        runTest {
            withApplicationScope { scope ->
                val reminder = FakeDailyReminderRepository().apply { markServerReminderHandedOff() }
                val notifications = RecordingNotifications()
                val coordinator =
                    DailyReminderCoordinator(
                        repository = reminder,
                        observeAuthSession = ObserveAuthSessionUseCase(FakeAuthRepository(AuthSessionState.Authenticated)),
                        handOffDailyReminder = HandOffDailyReminderUseCase(FakePushSettingsRepository(), reminder),
                        scheduler = RecordingAlarms(),
                        applicationScope = scope,
                    )
                val delivery =
                    DailyReminderDelivery(
                        repository = reminder,
                        coordinator = coordinator,
                        notifier = notifications,
                        applicationScope = scope,
                    )
                val scheduledAt = System.currentTimeMillis()
                var finished = 0

                // 둘 다 "아직 안 띄움"을 읽을 수 있게 기록 읽기에서 양보한다(대역 참고).
                delivery.onAlarm(scheduledAt) { finished++ }
                delivery.onAlarm(scheduledAt) { finished++ }
                advanceUntilIdle()

                assertEquals(2, finished)
                assertEquals(1, notifications.posted.size)
            }
        }
}
