package com.soma369.laimory.reminder

import com.soma369.laimory.analytics.RecordingAnalyticsHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPushDelivery
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPushType
import com.soma369.laimory.core.domain.model.analytics.AnalyticsReminderCopy
import com.soma369.laimory.core.domain.model.analytics.AnalyticsReminderKind
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import com.soma369.laimory.core.domain.usecase.reminder.HandOffDailyReminderUseCase
import kotlinx.coroutines.CoroutineScope
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
                val reminder = FakeDailyReminderRepository().apply { markServerReminderHandedOff(accountGeneration()) }
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
                        analyticsHelper = RecordingAnalyticsHelper(),
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

    @Test
    fun `띄우면 알림에 보인 문구로 표시 이벤트를 보낸다`() =
        runTest {
            withApplicationScope { scope ->
                val analytics = RecordingAnalyticsHelper()
                val delivery = delivery(scope, RecordingNotifications(), analytics)

                delivery.onAlarm(System.currentTimeMillis()) {}
                advanceUntilIdle()

                assertEquals(
                    listOf(
                        AnalyticsEvent.PushShown(
                            AnalyticsPushType.DAILY_REMINDER,
                            AnalyticsPushDelivery.LOCAL,
                            AnalyticsReminderCopy(AnalyticsReminderKind.GENERAL, "일반 0"),
                        ),
                    ),
                    analytics.logged,
                )
            }
        }

    @Test
    fun `기기 알림이 꺼져 띄우지 못하면 표시 이벤트를 보내지 않는다`() =
        runTest {
            withApplicationScope { scope ->
                val analytics = RecordingAnalyticsHelper()
                val delivery = delivery(scope, RecordingNotifications().apply { canPost = false }, analytics)

                delivery.onAlarm(System.currentTimeMillis()) {}
                advanceUntilIdle()

                assertEquals(emptyList<AnalyticsEvent>(), analytics.logged)
            }
        }

    private suspend fun delivery(
        scope: CoroutineScope,
        notifications: RecordingNotifications,
        analytics: RecordingAnalyticsHelper,
    ): DailyReminderDelivery {
        val reminder = FakeDailyReminderRepository().apply { markServerReminderHandedOff(accountGeneration()) }
        val coordinator =
            DailyReminderCoordinator(
                repository = reminder,
                observeAuthSession = ObserveAuthSessionUseCase(FakeAuthRepository(AuthSessionState.Authenticated)),
                handOffDailyReminder = HandOffDailyReminderUseCase(FakePushSettingsRepository(), reminder),
                scheduler = RecordingAlarms(),
                applicationScope = scope,
            )
        return DailyReminderDelivery(
            repository = reminder,
            coordinator = coordinator,
            notifier = notifications,
            analyticsHelper = analytics,
            applicationScope = scope,
        )
    }
}
