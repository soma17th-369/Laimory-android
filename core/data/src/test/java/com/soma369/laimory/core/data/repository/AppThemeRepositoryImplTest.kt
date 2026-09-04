package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.domain.model.settings.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeRepositoryImplTest {
    private val dataStore = InMemoryPreferencesDataStore()
    private val repository = AppThemeRepositoryImpl(dataStore)

    @Test
    fun `저장한 적이 없으면 시스템 설정을 따른다`() =
        runTest {
            assertEquals(AppThemeMode.SYSTEM, repository.themeMode.first())
        }

    @Test
    fun `고른 값을 저장하고 그대로 돌려준다`() =
        runTest {
            AppThemeMode.entries.forEach { mode ->
                repository.setThemeMode(mode)

                assertEquals(mode, repository.themeMode.first())
            }
        }

    @Test
    fun `알 수 없는 값이 저장돼 있으면 시스템 설정으로 떨어진다`() =
        runTest {
            // 저장 형식이 바뀌거나 값이 깨져도 앱이 열리지 않는 일은 없어야 한다.
            dataStore.updateData { mutablePreferencesOf(KEY_THEME_MODE to "NEON") }

            assertEquals(AppThemeMode.SYSTEM, repository.themeMode.first())
        }

    @Test
    fun `값이 바뀌면 흐름이 다시 방출한다`() =
        runTest {
            // 앱 루트와 설정 화면이 같은 흐름을 보므로, 저장되는 순간 둘 다 바뀌어야 한다.
            assertEquals(AppThemeMode.SYSTEM, repository.themeMode.first())

            repository.setThemeMode(AppThemeMode.DARK)

            assertEquals(AppThemeMode.DARK, repository.themeMode.first())
        }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
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
