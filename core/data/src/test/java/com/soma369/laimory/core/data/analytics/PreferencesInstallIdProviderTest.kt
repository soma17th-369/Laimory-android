package com.soma369.laimory.core.data.analytics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PreferencesInstallIdProviderTest {
    @Test
    fun `같은 설치에서는 같은 값을 돌려준다`() =
        runTest {
            val provider = PreferencesInstallIdProvider(InMemoryPreferencesDataStore())

            assertEquals(provider.get(), provider.get())
        }

    @Test
    fun `프로세스를 다시 만들어도 저장된 값을 돌려준다`() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val first = PreferencesInstallIdProvider(store).get()

            assertEquals(first, PreferencesInstallIdProvider(store).get())
        }

    @Test
    fun `저장소가 새로 생기면 새 값을 만든다`() =
        runTest {
            // 재설치는 백업에서 제외된 이 저장소가 비어 시작한다는 뜻이다.
            val before = PreferencesInstallIdProvider(InMemoryPreferencesDataStore()).get()
            val after = PreferencesInstallIdProvider(InMemoryPreferencesDataStore()).get()

            assertNotEquals(before, after)
        }

    @Test
    fun `설치 유입 귀속이 쓰는 값과 같다`() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val fromProvider = PreferencesInstallIdProvider(store).get()

            val fromAttribution = PreferencesInstallAttributionRepository(store).load().installId

            assertEquals(fromProvider, fromAttribution)
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
