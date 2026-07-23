package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ActiveDraftTaskRepositoryImplTest {
    private val dataStore = InMemoryPreferencesDataStore()
    private val repository = ActiveDraftTaskRepositoryImpl(dataStore)

    @Test
    fun `활성 작업의 최소 정보를 원자적으로 저장하고 복원한다`() =
        runTest {
            val task =
                com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask(
                    taskId = "task-1",
                    recordDate = LocalDate.of(2026, 7, 22),
                    requestedAt = Instant.parse("2026-07-22T01:02:03Z"),
                )

            repository.save(task)

            assertEquals(task, repository.get())
            assertEquals(3, dataStore.current.asMap().size)
        }

    @Test
    fun `필드가 일부만 있거나 형식이 잘못되면 복구하지 않는다`() =
        runTest {
            dataStore.updateData {
                mutablePreferencesOf(
                    stringPreferencesKey("task_id") to "task-1",
                    stringPreferencesKey("record_date") to "not-a-date",
                    longPreferencesKey("requested_at_epoch_millis") to 1L,
                )
            }

            assertNull(repository.get())
        }

    @Test
    fun `clear는 활성 작업 필드를 함께 제거한다`() =
        runTest {
            repository.save(
                com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask(
                    taskId = "task-1",
                    recordDate = LocalDate.of(2026, 7, 22),
                    requestedAt = Instant.EPOCH,
                ),
            )

            repository.clear()

            assertNull(repository.get())
            assertEquals(0, dataStore.current.asMap().size)
        }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())
        private val mutex = Mutex()

        val current: Preferences get() = state.value

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock {
                transform(state.value).also { state.value = it }
            }
    }
}
