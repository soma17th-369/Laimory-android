package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.data.datasource.remote.OnboardingRemoteDataSource
import com.soma369.laimory.core.data.di.OnboardingDataStore
import com.soma369.laimory.core.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

internal class OnboardingRepositoryImpl
    @Inject
    constructor(
        @OnboardingDataStore private val dataStore: DataStore<Preferences>,
        private val remoteDataSource: OnboardingRemoteDataSource,
    ) : OnboardingRepository {
        override suspend fun cachedCompletion(): Boolean? = preferences().first()[KEY_COMPLETED]

        override suspend fun cacheCompletion(isCompleted: Boolean) {
            dataStore.edit { preferences -> preferences[KEY_COMPLETED] = isCompleted }
        }

        override suspend fun recordCompletion() = remoteDataSource.recordCompletion()

        /**
         * 실패를 값으로 돌려준다.
         *
         * 조회는 앱 진입 판정에 쓰이므로 예외를 그대로 올리면 루트를 정하지 못해 로딩에서 멈춘다.
         * 서버는 설정 행이 없는 사용자에게 기본값 대신 500 을 내므로, 실패가 드물지도 않다.
         */
        override suspend fun fetchCompletion(): Result<Boolean> = runCatching { remoteDataSource.fetchCompletion() }

        override fun observeLastPageKey(): Flow<String?> =
            preferences().map { preferences -> preferences[KEY_LAST_PAGE_KEY]?.takeIf(String::isNotBlank) }

        override suspend fun saveProgress(pageKey: String) {
            dataStore.edit { preferences -> preferences[KEY_LAST_PAGE_KEY] = pageKey }
        }

        override suspend fun clear() {
            dataStore.edit { preferences -> preferences.clear() }
        }

        /** 읽기 실패는 빈 값으로 떨어뜨린다. 여기서 예외를 올리면 앱 루트를 정하지 못한다. */
        private fun preferences(): Flow<Preferences> =
            dataStore.data.catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }

        private companion object {
            val KEY_COMPLETED = booleanPreferencesKey("is_completed")
            val KEY_LAST_PAGE_KEY = stringPreferencesKey("last_page_key")
        }
    }
