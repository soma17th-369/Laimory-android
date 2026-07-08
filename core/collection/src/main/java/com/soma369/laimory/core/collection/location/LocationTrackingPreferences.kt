package com.soma369.laimory.core.collection.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "위치 추적을 원함" 토글 의도의 영속 저장(단일 소스). 리포지토리·서비스·부팅 리시버가 공유한다.
 *
 * Phase 2 는 백그라운드/부팅 복원을 위해 의도를 영속한다(Phase 1 의 세션 한정·콜드스타트 off 를 대체).
 */
@Singleton
internal class LocationTrackingPreferences
    @Inject
    constructor(
        @LocationTrackingDataStore private val dataStore: DataStore<Preferences>,
    ) {
        fun observeEnabled(): Flow<Boolean> = dataStore.data.map { prefs -> prefs[KEY_ENABLED] ?: false }

        suspend fun isEnabled(): Boolean = observeEnabled().first()

        suspend fun setEnabled(enabled: Boolean) {
            dataStore.edit { prefs -> prefs[KEY_ENABLED] = enabled }
        }

        private companion object {
            val KEY_ENABLED = booleanPreferencesKey("enabled")
        }
    }
