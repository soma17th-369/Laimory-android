package com.soma369.laimory.core.data.analytics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.soma369.laimory.core.domain.model.analytics.AnalyticsTimelineEditLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PreferencesAnalyticsTimelineEditLogRepositoryTest {
    private val date = LocalDate.parse("2026-09-21")
    private val otherDate = LocalDate.parse("2026-09-20")
    private val repository = PreferencesAnalyticsTimelineEditLogRepository(InMemoryPreferencesDataStore())

    @Test
    fun `같은 이벤트를 여러 번 고쳐도 한 건이다`() =
        runTest {
            repository.markEdited(date, 1L)
            repository.markEdited(date, 1L)
            repository.markEdited(date, 2L)

            assertEquals(setOf(1L, 2L), repository.take(date).editedEventIds)
        }

    @Test
    fun `지운 AI 이벤트를 따로 모은다`() =
        runTest {
            repository.markDeletedAi(date, 3L)

            assertEquals(AnalyticsTimelineEditLog(editedEventIds = emptySet(), deletedAiEventIds = setOf(3L)), repository.take(date))
        }

    @Test
    fun `꺼내면 비운다`() =
        runTest {
            repository.markEdited(date, 1L)
            repository.markDeletedAi(date, 3L)

            repository.take(date)

            assertEquals(AnalyticsTimelineEditLog.EMPTY, repository.take(date))
        }

    @Test
    fun `날짜마다 따로 둔다`() =
        runTest {
            repository.markEdited(date, 1L)
            repository.markEdited(otherDate, 2L)

            repository.take(date)

            assertEquals(setOf(2L), repository.take(otherDate).editedEventIds)
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
