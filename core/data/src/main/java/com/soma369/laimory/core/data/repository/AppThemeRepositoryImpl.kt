package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.data.di.AppSettingsDataStore
import com.soma369.laimory.core.domain.model.settings.AppThemeMode
import com.soma369.laimory.core.domain.repository.AppThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

internal class AppThemeRepositoryImpl
    @Inject
    constructor(
        @AppSettingsDataStore private val dataStore: DataStore<Preferences>,
    ) : AppThemeRepository {
        /**
         * 읽기 실패는 기본값으로 떨어뜨린다.
         *
         * 이 흐름이 앱 루트의 테마를 정하므로, 예외를 올리면 화면을 그릴 색을 정하지 못한다.
         */
        override val themeMode: Flow<AppThemeMode> =
            dataStore.data
                .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                .map { preferences -> AppThemeMode.fromName(preferences[KEY_THEME_MODE]) }

        override suspend fun setThemeMode(mode: AppThemeMode) {
            dataStore.edit { preferences -> preferences[KEY_THEME_MODE] = mode.name }
        }

        private companion object {
            val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        }
    }
