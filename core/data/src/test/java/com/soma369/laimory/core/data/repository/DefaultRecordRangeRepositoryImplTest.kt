package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.domain.model.settings.DefaultRecordRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class DefaultRecordRangeRepositoryImplTest {
    private val dataStore = InMemoryPreferencesDataStore()
    private val repository = DefaultRecordRangeRepositoryImpl(dataStore)

    @Test
    fun `저장한 적이 없으면 06시부터 익일 06시다`() =
        runTest {
            assertEquals(DefaultRecordRange.INITIAL, repository.range.first())
            assertEquals(LocalTime.of(6, 0), DefaultRecordRange.INITIAL.startTime)
            assertEquals(true, DefaultRecordRange.INITIAL.endsNextDay)
            assertEquals(LocalTime.of(6, 0), DefaultRecordRange.INITIAL.endTime)
        }

    @Test
    fun `저장한 범위를 그대로 돌려준다`() =
        runTest {
            val sameDay = DefaultRecordRange(LocalTime.of(9, 30), endsNextDay = false, endTime = LocalTime.of(23, 55))
            val nextDay = DefaultRecordRange(LocalTime.of(21, 0), endsNextDay = true, endTime = LocalTime.of(3, 5))

            repository.setRange(sameDay)
            assertEquals(sameDay, repository.range.first())

            repository.setRange(nextDay)
            assertEquals(nextDay, repository.range.first())
        }

    @Test
    fun `키가 하나라도 빠져 있으면 저장한 적이 없는 것으로 본다`() =
        runTest {
            // 반쯤 남은 값으로 범위를 만들면 사용자가 고른 적 없는 범위가 된다.
            dataStore.updateData { mutablePreferencesOf(KEY_START_MINUTE to 9 * 60, KEY_ENDS_NEXT_DAY to true) }

            assertEquals(DefaultRecordRange.INITIAL, repository.range.first())
        }

    @Test
    fun `하루를 벗어난 분 값은 저장한 적이 없는 것으로 본다`() =
        runTest {
            dataStore.updateData {
                mutablePreferencesOf(KEY_START_MINUTE to 24 * 60, KEY_ENDS_NEXT_DAY to true, KEY_END_MINUTE to 6 * 60)
            }

            assertEquals(DefaultRecordRange.INITIAL, repository.range.first())
        }

    @Test
    fun `같은 파일의 다른 설정이 바뀌어도 다시 방출하지 않는다`() =
        runTest {
            // 화면 모드를 바꿀 때마다 홈이 같은 범위를 다시 받으면 할 일이 없는 갱신이 돈다.
            val emitted = mutableListOf<DefaultRecordRange>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { repository.range.take(2).toList(emitted) }

            dataStore.updateData { mutablePreferencesOf(stringPreferencesKey("theme_mode") to "DARK") }
            val saved = DefaultRecordRange(LocalTime.of(7, 0), endsNextDay = true, endTime = LocalTime.of(2, 0))
            repository.setRange(saved)
            job.join()

            assertEquals(listOf(DefaultRecordRange.INITIAL, saved), emitted)
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

    private companion object {
        val KEY_START_MINUTE = intPreferencesKey("default_record_range_start_minute")
        val KEY_ENDS_NEXT_DAY = booleanPreferencesKey("default_record_range_ends_next_day")
        val KEY_END_MINUTE = intPreferencesKey("default_record_range_end_minute")
    }
}
