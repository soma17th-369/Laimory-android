package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.data.di.ActiveDraftTaskDataStore
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.repository.ActiveDraftTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

internal class ActiveDraftTaskRepositoryImpl
    @Inject
    constructor(
        @ActiveDraftTaskDataStore private val dataStore: DataStore<Preferences>,
    ) : ActiveDraftTaskRepository {
        override fun observe(): Flow<ActiveDraftTask?> =
            dataStore.data
                .catch { error ->
                    if (error is IOException) emit(emptyPreferences()) else throw error
                }.map(::toActiveTaskOrNull)

        override suspend fun get(): ActiveDraftTask? = observe().first()

        override suspend fun save(task: ActiveDraftTask) {
            dataStore.edit { preferences ->
                preferences[KEY_TASK_ID] = task.taskId
                preferences[KEY_RECORD_DATE] = task.recordDate.toString()
                preferences[KEY_REQUESTED_AT] = task.requestedAt.toEpochMilli()
            }
        }

        override suspend fun clear() {
            dataStore.edit { preferences ->
                preferences.remove(KEY_TASK_ID)
                preferences.remove(KEY_RECORD_DATE)
                preferences.remove(KEY_REQUESTED_AT)
            }
        }

        private fun toActiveTaskOrNull(preferences: Preferences): ActiveDraftTask? {
            val taskId = preferences[KEY_TASK_ID]?.takeIf(String::isNotBlank) ?: return null
            val recordDate = preferences[KEY_RECORD_DATE] ?: return null
            val requestedAt = preferences[KEY_REQUESTED_AT] ?: return null
            return runCatching {
                ActiveDraftTask(
                    taskId = taskId,
                    recordDate = LocalDate.parse(recordDate),
                    requestedAt = Instant.ofEpochMilli(requestedAt),
                )
            }.getOrNull()
        }

        private companion object {
            val KEY_TASK_ID = stringPreferencesKey("task_id")
            val KEY_RECORD_DATE = stringPreferencesKey("record_date")
            val KEY_REQUESTED_AT = longPreferencesKey("requested_at_epoch_millis")
        }
    }
