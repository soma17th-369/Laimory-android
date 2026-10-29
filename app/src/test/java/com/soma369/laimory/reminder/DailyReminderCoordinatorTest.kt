package com.soma369.laimory.reminder

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.reminder.DailyReminderSettings
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import com.soma369.laimory.core.domain.usecase.reminder.HandOffDailyReminderUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyReminderCoordinatorTest {
    private val auth = FakeAuthRepository(AuthSessionState.Authenticated)
    private val server = FakePushSettingsRepository()
    private val reminder = FakeDailyReminderRepository()
    private val alarms = RecordingAlarms()

    @Test
    fun `서버 끄기가 끝나야 예약한다`() =
        runTest {
            withApplicationScope { scope ->
                server.updateFailure = ApiException.NetworkException()

                coordinator(scope).start()
                advanceUntilIdle()

                assertFalse(reminder.isHandedOff)
                assertNull(alarms.scheduledTime)

                server.updateFailure = null
                coordinator(scope).retryHandOffIfNeeded()
                advanceUntilIdle()

                assertTrue(reminder.isHandedOff)
                assertEquals(DailyReminderSettings.DEFAULT_TIME, alarms.scheduledTime)
            }
        }

    @Test
    fun `다른 계정으로 바꾸면 그 계정의 서버 끄기가 끝날 때까지 예약하지 않는다`() =
        runTest {
            withApplicationScope { scope ->
                // A 계정: 넘겨받기 성공 → 예약.
                val coordinator = coordinator(scope)
                coordinator.start()
                advanceUntilIdle()
                assertEquals(DailyReminderSettings.DEFAULT_TIME, alarms.scheduledTime)

                // 로그아웃: 예약을 내리고 A 의 넘겨받기 표시를 비운다.
                auth.state.value = AuthSessionState.Unauthenticated
                advanceUntilIdle()
                assertNull(alarms.scheduledTime)
                assertFalse(reminder.isHandedOff)

                // B 계정: 서버 리마인더가 켜져 있고 끄기가 실패한다. A 의 표시로 예약을 허락받으면 서버 알림과
                // 기기 알림이 함께 온다.
                server.settings = server.settings.copy(isDailyReminderEnabled = true)
                server.updateFailure = ApiException.NetworkException()
                auth.state.value = AuthSessionState.Authenticated
                advanceUntilIdle()
                assertFalse(reminder.isHandedOff)
                assertNull(alarms.scheduledTime)

                // 전경 복귀 재시도: 이번엔 성공해 B 의 표시로 예약한다.
                server.updateFailure = null
                coordinator.retryHandOffIfNeeded()
                advanceUntilIdle()
                assertTrue(reminder.isHandedOff)
                assertEquals(DailyReminderSettings.DEFAULT_TIME, alarms.scheduledTime)
                assertEquals(listOf(false, false, false), server.dailyReminderUpdates)
            }
        }

    @Test
    fun `전체 알림이 꺼진 계정이면 예약하지 않는다`() =
        runTest {
            withApplicationScope { scope ->
                server.settings = server.settings.copy(isPushEnabled = false)

                coordinator(scope).start()
                advanceUntilIdle()

                assertTrue(reminder.isHandedOff)
                assertNull(alarms.scheduledTime)
            }
        }

    private fun coordinator(applicationScope: CoroutineScope) =
        DailyReminderCoordinator(
            repository = reminder,
            observeAuthSession = ObserveAuthSessionUseCase(auth),
            handOffDailyReminder = HandOffDailyReminderUseCase(server, reminder),
            scheduler = alarms,
            applicationScope = applicationScope,
        )
}
