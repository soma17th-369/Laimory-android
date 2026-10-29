package com.soma369.laimory.core.domain.usecase.reminder

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.push.PushSettings
import com.soma369.laimory.core.domain.model.reminder.DailyReminderSettings
import com.soma369.laimory.core.domain.repository.PushSettingsRepository
import com.soma369.laimory.core.domain.usecase.FakeDailyReminderRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandOffDailyReminderUseCaseTest {
    private val server = FakePushSettingsRepository()
    private val reminder = FakeDailyReminderRepository()
    private val useCase = HandOffDailyReminderUseCase(server, reminder)

    @Test
    fun `서버 리마인더가 켜져 있으면 끄고 넘겨받는다`() =
        runBlocking {
            server.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = true)

            assertTrue(useCase().isSuccess)

            assertEquals(listOf(false), server.dailyReminderUpdates)
            assertTrue(reminder.isHandedOff)
            assertEquals(true, reminder.mirrorValue)
        }

    @Test
    fun `이미 꺼져 있으면 서버에 보내지 않고 넘겨받는다`() =
        runBlocking {
            server.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)
            reminder.storedEnabled = true

            useCase()

            assertTrue(server.dailyReminderUpdates.isEmpty())
            assertTrue(reminder.isHandedOff)
        }

    @Test
    fun `기기 값이 없으면 서버의 예전 선택을 이어받는다`() =
        runBlocking {
            // 꺼 두었던 사람에게 다시 켜서 보내지 않는다.
            server.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)

            useCase()

            assertFalse(reminder.currentSettings.isEnabled)
            assertEquals(DailyReminderSettings.DEFAULT_TIME, reminder.currentSettings.time)
        }

    @Test
    fun `기기 값이 있으면 서버 값은 보지 않는다`() =
        runBlocking {
            // 전환 뒤에는 모든 계정의 서버 값이 꺼짐이라, 이어받으면 켜 둔 사람도 꺼진다.
            reminder.setEnabled(true)
            server.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = false)

            useCase()

            assertTrue(reminder.currentSettings.isEnabled)
        }

    @Test
    fun `서버 끄기가 실패하면 넘겨받지 않는다`() =
        runBlocking {
            // 넘겨받았다고 적으면 기기도 예약해, 서버가 아직 보내는 동안 같은 시각에 두 번 온다.
            server.settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = true)
            server.updateFailure = ApiException.NetworkException()

            assertTrue(useCase().isFailure)

            assertFalse(reminder.isHandedOff)
        }

    @Test
    fun `조회가 실패하면 아무것도 바꾸지 않는다`() =
        runBlocking {
            server.getFailure = ApiException.NetworkException()

            assertTrue(useCase().isFailure)

            assertFalse(reminder.isHandedOff)
            assertEquals(null, reminder.mirrorValue)
            assertFalse(reminder.storedEnabled)
        }

    @Test
    fun `기다리는 사이 세션이 바뀌면 앞 결과를 버리고 지금 세션으로 다시 한다`() =
        runBlocking {
            // 첫 조회 응답을 기다리는 사이 다른 계정으로 로그인했다.
            var calls = 0
            server.onGet = { if (calls++ == 0) reminder.prepareNewSession() }

            assertTrue(useCase().isSuccess)

            assertEquals(2, calls)
            assertTrue(reminder.isHandedOff)
        }

    @Test
    fun `앞 세션의 늦은 응답은 넘겨받음으로 적지 않는다`() =
        runBlocking {
            // 세션이 계속 바뀌어 재시도도 모두 묵은 결과다.
            server.onGet = { reminder.prepareNewSession() }

            assertTrue(useCase().isFailure)

            assertFalse(reminder.isHandedOff)
            assertEquals(null, reminder.mirrorValue)
        }

    private class FakePushSettingsRepository : PushSettingsRepository {
        var settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = true)
        var getFailure: ApiException? = null
        var onGet: suspend () -> Unit = {}
        var updateFailure: ApiException? = null
        val dailyReminderUpdates = mutableListOf<Boolean>()

        override suspend fun getPushSettings(): PushSettings {
            onGet()
            getFailure?.let { throw it }
            return settings
        }

        override suspend fun updatePushEnabled(isEnabled: Boolean) = Unit

        override suspend fun updateDailyReminderEnabled(isEnabled: Boolean) {
            dailyReminderUpdates += isEnabled
            updateFailure?.let { throw it }
        }
    }
}
