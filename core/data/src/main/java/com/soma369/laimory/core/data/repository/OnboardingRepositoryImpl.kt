package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.data.datasource.remote.OnboardingRemoteDataSource
import com.soma369.laimory.core.data.di.OnboardingDataStore
import com.soma369.laimory.core.domain.model.onboarding.OnboardingState
import com.soma369.laimory.core.domain.repository.OnboardingRepository
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

internal class OnboardingRepositoryImpl
    @Inject
    constructor(
        @OnboardingDataStore private val dataStore: DataStore<Preferences>,
        private val remoteDataSource: OnboardingRemoteDataSource,
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
         *
         * **로컬을 먼저 확정하고 서버 기록은 best-effort 다.** 자동 노출 판정의 정본이 설치 단위
         * 로컬이라, 서버가 안 되는 동안 사용자를 온보딩에 묶어 둘 이유가 없다.
         *
         * 서버 기록 실패는 흐름을 막지 않고 로그로만 남긴다. 계정 단위 이력이라 화면 동작에
         * 영향이 없다. 다만 오프라인 완료가 서버에 안 남을 수 있는 것은 알려진 한계다 —
         * 재시도가 필요해지면 `완료했지만 아직 못 올림` 을 저장하고 다음 실행에 다시 올리는
         * 편이 맞다(API 는 멱등이다).
         */
        override suspend fun complete() {
            dataStore.edit { preferences -> preferences[KEY_COMPLETED] = true }
            runCatching { remoteDataSource.recordCompletion() }
                .onFailure { cause ->
                    // 응답 본문에 계정 정보가 실릴 수 있어 메시지 대신 예외 종류만 남긴다.
                    Logger.w(LogDomain.NETWORK, "온보딩 완료 서버 기록 실패: ${cause.javaClass.simpleName}")
                }
        }

        /**
         * 로컬만 되돌린다.
         *
         * 서버에는 `false` 로 되돌리는 API 가 없다. 계정 단위 이력은 완료로 남아 있고 설치 단위
         * 판정만 처음으로 간다 — QA 가 흐름을 다시 보는 데는 이것으로 충분하다.
         */
        override suspend fun reset() {
            dataStore.edit { preferences -> preferences.clear() }
        }

        private companion object {
            val KEY_COMPLETED = booleanPreferencesKey("is_completed")
            val KEY_LAST_PAGE_KEY = stringPreferencesKey("last_page_key")
        }
    }
