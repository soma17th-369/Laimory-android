package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.soma369.laimory.core.data.di.AppSettingsDataStore
import com.soma369.laimory.core.domain.model.update.DismissedRecommendation
import com.soma369.laimory.core.domain.repository.AppUpdateRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

/**
 * 앱 설정 저장소를 함께 쓴다. 보류는 계정이 아니라 **이 설치**가 미룬 것이라 로그아웃해도 남는다.
 */
internal class AppUpdateRepositoryImpl
    @Inject
    constructor(
        @AppSettingsDataStore private val dataStore: DataStore<Preferences>,
    ) : AppUpdateRepository {
        /**
         * 읽기 실패는 "미룬 적 없음" 으로 떨어뜨린다.
         *
         * 안내를 한 번 더 보여 주는 쪽이, 읽지 못한 기록 때문에 영영 감추는 쪽보다 낫다.
         */
        override suspend fun dismissedRecommendation(): DismissedRecommendation? {
            val preferences =
                dataStore.data
                    .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                    .first()
            val version = preferences[KEY_DISMISSED_VERSION] ?: return null
            val at = preferences[KEY_DISMISSED_AT] ?: return null
            return DismissedRecommendation(version = version, at = Instant.ofEpochMilli(at))
        }

        override suspend fun dismissRecommendation(
            version: Int,
            at: Instant,
        ) {
            dataStore.edit { preferences ->
                preferences[KEY_DISMISSED_VERSION] = version
                preferences[KEY_DISMISSED_AT] = at.toEpochMilli()
            }
        }

        private companion object {
            val KEY_DISMISSED_VERSION = intPreferencesKey("dismissed_recommend_version")
            val KEY_DISMISSED_AT = longPreferencesKey("dismissed_recommend_at")
        }
    }
