package com.soma369.laimory.core.collection.health.sleep.detection

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
 * "수면 자동 감지를 원함" 의도의 영속 저장(단일 소스). 리포지토리·구독자·부팅 리시버가 공유한다.
 *
 * 콜드스타트/부팅 시 이 의도를 읽어 구독을 복원한다([SleepDetectionSubscriber.startIfEnabled]).
 */
@Singleton
internal class SleepDetectionPreferences
    @Inject
    constructor(
        @SleepDetectionDataStore private val dataStore: DataStore<Preferences>,
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
