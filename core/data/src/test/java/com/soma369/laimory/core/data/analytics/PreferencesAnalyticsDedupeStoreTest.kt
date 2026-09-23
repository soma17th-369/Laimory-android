package com.soma369.laimory.core.data.analytics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferencesAnalyticsDedupeStoreTest {
    private val store = PreferencesAnalyticsDedupeStore(InMemoryPreferencesDataStore())

    @Test
    fun `처음 보는 키만 처음이다`() =
        runTest {
            assertTrue(store.markIfFirst("timeline_completed:2026-09-18:2"))
            assertFalse(store.markIfFirst("timeline_completed:2026-09-18:2"))
        }

    @Test
    fun `뿌리 키를 지우면 거기서 갈라진 회원 키도 지운다`() =
        runTest {
            // 지우는 쪽은 회원을 모를 수 있다. 뿌리로 한 번에 지워야 남은 판정이 다음 기록을 막지 않는다.
            store.markIfFirst("timeline_completed:2026-09-18")
            store.markIfFirst("timeline_completed:2026-09-18:2")
            store.markIfFirst("timeline_completed:2026-09-18:7")

            store.forgetFamily("timeline_completed:2026-09-18")

            assertTrue(store.markIfFirst("timeline_completed:2026-09-18"))
            assertTrue(store.markIfFirst("timeline_completed:2026-09-18:2"))
            assertTrue(store.markIfFirst("timeline_completed:2026-09-18:7"))
        }

    @Test
    fun `다른 날짜 판정은 건드리지 않는다`() =
        runTest {
            store.markIfFirst("timeline_completed:2026-09-17:2")

            store.forgetFamily("timeline_completed:2026-09-18")

            assertFalse(store.markIfFirst("timeline_completed:2026-09-17:2"))
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
