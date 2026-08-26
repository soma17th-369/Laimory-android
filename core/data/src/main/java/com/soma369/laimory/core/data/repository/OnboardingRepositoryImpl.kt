package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.data.di.OnboardingDataStore
import com.soma369.laimory.core.domain.model.onboarding.OnboardingState
import com.soma369.laimory.core.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

internal class OnboardingRepositoryImpl
    @Inject
    constructor(
        @OnboardingDataStore private val dataStore: DataStore<Preferences>,
    ) : OnboardingRepository {
        /**
         * 읽기 실패는 "아직 안 함" 으로 떨어뜨린다.
         *
         * 여기서 예외를 올리면 앱 루트를 정하지 못해 로딩에서 멈춘다. 최악의 오동작은 온보딩을
         * 한 번 더 보는 것이고, 그 편이 앱이 열리지 않는 것보다 낫다.
         */
        override fun observe(): Flow<OnboardingState> =
            dataStore.data
                .catch { error ->
                    if (error is IOException) emit(emptyPreferences()) else throw error
                }.map { preferences ->
                    OnboardingState(
                        isCompleted = preferences[KEY_COMPLETED] == true,
                        lastPageKey = preferences[KEY_LAST_PAGE_KEY]?.takeIf(String::isNotBlank),
                    )
                }

        override suspend fun saveProgress(pageKey: String) {
            dataStore.edit { preferences -> preferences[KEY_LAST_PAGE_KEY] = pageKey }
        }

        /**
         * 완료만 세우고 마지막 페이지는 남긴다.
         *
         * 완료 뒤에도 설정에서 다시 열 수 있으므로 진행 흔적을 지울 이유가 없고, 지우면 완료
         * 저장이 두 키를 건드리는 만큼 부분 실패 여지가 늘어난다.
         */
        override suspend fun complete() {
            dataStore.edit { preferences -> preferences[KEY_COMPLETED] = true }
        }

        override suspend fun reset() {
            dataStore.edit { preferences -> preferences.clear() }
        }

        private companion object {
            val KEY_COMPLETED = booleanPreferencesKey("is_completed")
            val KEY_LAST_PAGE_KEY = stringPreferencesKey("last_page_key")
        }
    }
