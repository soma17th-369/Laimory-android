package com.soma369.laimory.core.collection.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** 추적 토글을 DataStore 에 저장하고, 상태에 따라 [LocationTrackingManager] 를 시작/중지한다. */
internal class LocationTrackingRepositoryImpl
    @Inject
    constructor(
        @LocationTrackingDataStore private val dataStore: DataStore<Preferences>,
        private val manager: LocationTrackingManager,
    ) : LocationTrackingRepository {
        override fun observeEnabled(): Flow<Boolean> = dataStore.data.map { prefs -> prefs[KEY_ENABLED] ?: false }

        override fun observeStatus(): Flow<LocationTrackingStatus?> = manager.status

        override suspend fun setEnabled(enabled: Boolean) {
            dataStore.edit { prefs -> prefs[KEY_ENABLED] = enabled }
            if (enabled) manager.start() else manager.stop()
        }

        private companion object {
            val KEY_ENABLED = booleanPreferencesKey("enabled")
        }
    }
