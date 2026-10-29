package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.soma369.laimory.core.domain.model.reminder.DailyReminderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DailyReminderRepositoryImplTest {
    private val repository = DailyReminderRepositoryImpl(InMemoryPreferencesDataStore())
    private val recordDate = LocalDate.of(2026, 10, 30)

    @Test
    fun `지금 세대로는 계정 값을 적는다`() =
        runTest {
            val generation = repository.accountGeneration()

            assertTrue(repository.markServerReminderHandedOff(generation))
            repository.setPushEnabledMirror(isEnabled = false, generation = generation)
            repository.markRecordSaved(recordDate, generation)

            assertTrue(repository.isServerReminderHandedOff.first())
            assertEquals(false, repository.pushEnabledMirror.first())
            assertEquals(recordDate, repository.deliveryMemory().lastSavedRecordDate)
        }

    @Test
    fun `새 세션이 시작되면 계정 값을 비우고 앞 세대의 늦은 쓰기는 버린다`() =
        runTest {
            val previous = repository.accountGeneration()
            repository.markServerReminderHandedOff(previous)
            repository.setPushEnabledMirror(isEnabled = true, generation = previous)
            repository.markRecordSaved(recordDate, previous)

            repository.prepareNewSession()

            assertFalse(repository.isServerReminderHandedOff.first())
            assertNull(repository.pushEnabledMirror.first())
            assertNull(repository.deliveryMemory().lastSavedRecordDate)

            // 앞 세션 요청이 늦게 끝나 적으려 한다.
            assertFalse(repository.markServerReminderHandedOff(previous))
            repository.setPushEnabledMirror(isEnabled = false, generation = previous)
            repository.markRecordSaved(recordDate, previous)

            assertFalse(repository.isServerReminderHandedOff.first())
            assertNull(repository.pushEnabledMirror.first())
            assertNull(repository.deliveryMemory().lastSavedRecordDate)
        }

    @Test
    fun `새 세션이 시작돼도 켜짐과 시각은 남긴다`() =
        runTest {
            // 기기를 쓰는 사람이 정한 값이다.
            repository.setEnabled(false)
            repository.setTime(LocalTime.of(22, 30))

            repository.prepareNewSession()

            assertEquals(DailyReminderSettings(isEnabled = false, time = LocalTime.of(22, 30)), repository.settings.first())
            assertTrue(repository.hasStoredEnabled())
        }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())
        private val mutex = Mutex()

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock {
                transform(state.value).also { state.value = it }
            }
    }
}
