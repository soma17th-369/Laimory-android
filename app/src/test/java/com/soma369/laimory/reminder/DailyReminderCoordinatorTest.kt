package com.soma369.laimory.reminder

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.reminder.DailyReminderSettings
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import com.soma369.laimory.core.domain.usecase.reminder.HandOffDailyReminderUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

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
    fun `다른 계정으로 로그인하면 그 계정의 서버 끄기가 끝날 때까지 예약하지 않는다`() =
        runTest {
            withApplicationScope { scope ->
                // A 계정: 넘겨받기 성공 → 예약.
                val coordinator = coordinator(scope)
                coordinator.start()
                advanceUntilIdle()
                assertEquals(DailyReminderSettings.DEFAULT_TIME, alarms.scheduledTime)

                // 로그아웃: 예약을 내린다.
                auth.state.value = AuthSessionState.Unauthenticated
                advanceUntilIdle()
                assertNull(alarms.scheduledTime)

                // B 계정 로그인: 서버 리마인더가 켜져 있고 끄기가 실패한다. A 의 표시로 예약을 허락받으면 서버 알림과
                // 기기 알림이 함께 온다.
                server.settings = server.settings.copy(isDailyReminderEnabled = true)
                server.updateFailure = ApiException.NetworkException()
                loginAs()
                advanceUntilIdle()
                assertFalse(reminder.isHandedOff)
                assertNull(alarms.scheduledTime)

                // 전경 복귀 재시도: 이번엔 성공해 B 의 표시로 예약한다.
                server.updateFailure = null
                coordinator.retryHandOffIfNeeded()
                advanceUntilIdle()
                assertTrue(reminder.isHandedOff)
                assertEquals(DailyReminderSettings.DEFAULT_TIME, alarms.scheduledTime)
            }
        }

    @Test
    fun `앞 세션 응답을 기다리는 사이 로그아웃하고 다시 로그인해도 늦은 응답으로 예약하지 않는다`() =
        runTest {
            withApplicationScope { scope ->
                // A 는 예전에 넘겨받아 예약돼 있다. 앱을 다시 켜 A 의 넘겨받기가 조회 응답을 기다린다.
                reminder.markServerReminderHandedOff(reminder.accountGeneration())
                val gate = CompletableDeferred<Unit>()
                server.getGate = gate
                val coordinator = coordinator(scope)
                coordinator.start()
                advanceUntilIdle()
                assertEquals(DailyReminderSettings.DEFAULT_TIME, alarms.scheduledTime)

                // 응답이 오기 전에 로그아웃 → B 로그인. 둘 사이를 돌리지 않아 인증 상태는 "로그인" 그대로 보인다.
                auth.state.value = AuthSessionState.Unauthenticated
                loginAs()
                server.settings = server.settings.copy(isDailyReminderEnabled = true)
                server.updateFailure = ApiException.NetworkException()
                advanceUntilIdle()
                // 새 세션 몫으로 갈라 두어 예약은 이미 내려가 있다.
                assertNull(alarms.scheduledTime)

                // A 의 늦은 응답: 세대가 달라 버리고, 지금 세션(B)으로 다시 하지만 B 의 서버 끄기는 실패한다.
                gate.complete(Unit)
                advanceUntilIdle()
                assertFalse(reminder.isHandedOff)
                assertNull(alarms.scheduledTime)

                server.updateFailure = null
                coordinator.retryHandOffIfNeeded()
                advanceUntilIdle()
                assertTrue(reminder.isHandedOff)
                assertEquals(DailyReminderSettings.DEFAULT_TIME, alarms.scheduledTime)
            }
        }

    @Test
    fun `앞 세션 응답을 기다리는 사이 로그아웃하면 그 요청을 거두고 새 세션 것을 한다`() =
        runTest {
            withApplicationScope { scope ->
                reminder.markServerReminderHandedOff(reminder.accountGeneration())
                server.getGate = CompletableDeferred()
                coordinator(scope).start()
                advanceUntilIdle()

                // 로그아웃이 관찰되면 A 의 넘겨받기를 취소한다. 잠금을 쥔 채 서버를 기다리면 B 의 일이 그 뒤에 선다.
                auth.state.value = AuthSessionState.Unauthenticated
                advanceUntilIdle()
                assertNull(alarms.scheduledTime)

                // B 로그인: A 의 응답이 끝내 오지 않아도 B 의 넘겨받기가 바로 돈다.
                server.settings = server.settings.copy(isDailyReminderEnabled = true)
                loginAs()
                advanceUntilIdle()
                assertEquals(2, server.getCount)
                assertTrue(reminder.isHandedOff)
                assertEquals(DailyReminderSettings.DEFAULT_TIME, alarms.scheduledTime)
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

    @Test
    fun `켜진 채 시간대를 바꿀 때만 오늘 몫을 따라잡는다`() =
        runTest {
            withApplicationScope { scope ->
                coordinator(scope).start()
                advanceUntilIdle()
                // 앱 시작: 지난 시각이어도 따라잡지 않는다. 따라잡으면 앱을 열 때마다 알림이 뜬다.
                assertEquals(DailyReminderSettings.DEFAULT_TIME, alarms.scheduledTime)
                assertFalse(alarms.lastCatchUpToday)

                reminder.setTime(LocalTime.of(22, 0))
                advanceUntilIdle()
                assertEquals(LocalTime.of(22, 0), alarms.scheduledTime)
                assertTrue(alarms.lastCatchUpToday)

                // 껐다 켜기는 시간대 변경이 아니다.
                reminder.setEnabled(false)
                advanceUntilIdle()
                reminder.setEnabled(true)
                advanceUntilIdle()
                assertEquals(LocalTime.of(22, 0), alarms.scheduledTime)
                assertFalse(alarms.lastCatchUpToday)
            }
        }

    /** 로그인 경로(`IssueAuthTokensUseCase`)가 하는 일 — 토큰 저장 전에 새 세션 몫을 갈라 두고 로그인 상태가 된다. */
    private suspend fun loginAs() {
        reminder.prepareNewSession()
        auth.state.value = AuthSessionState.Authenticated
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
