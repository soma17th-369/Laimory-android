package com.soma369.laimory.core.data.analytics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.soma369.laimory.core.data.di.AnalyticsDataStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 판정 기록을 DataStore 에 남긴다.
 *
 * 읽고 쓰는 일을 `edit` 한 번 안에서 끝낸다 — 읽은 뒤 따로 쓰면 같은 사건을 동시에 관찰한 두
 * 경로(예: 푸시와 폴링)가 둘 다 "처음"으로 판정해 이벤트가 두 번 나간다.
 */
@Singleton
internal class PreferencesAnalyticsDedupeStore
    @Inject
    constructor(
        @AnalyticsDataStore private val dataStore: DataStore<Preferences>,
    ) : AnalyticsDedupeStore {
        override suspend fun markIfFirst(key: String): Boolean {
            var first = false
            dataStore.edit { preferences ->
                val preferenceKey = booleanPreferencesKey(key)
                first = preferences[preferenceKey] != true
                if (first) preferences[preferenceKey] = true
            }
            return first
        }
    }
