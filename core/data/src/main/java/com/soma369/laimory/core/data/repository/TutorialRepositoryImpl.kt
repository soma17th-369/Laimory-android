package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.soma369.laimory.core.data.di.AppSettingsDataStore
import com.soma369.laimory.core.domain.model.tutorial.Tutorial
import com.soma369.laimory.core.domain.repository.TutorialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * 튜토리얼을 봤는지 앱 설정 저장소에 둔다. 이 기기에서 한 번 본 안내라 로그아웃해도 남는다.
 *
 * 키는 튜토리얼마다 하나다. 저장한 적이 없으면 보지 않은 것이다.
 */
internal class TutorialRepositoryImpl
    @Inject
    constructor(
        @AppSettingsDataStore private val dataStore: DataStore<Preferences>,
    ) : TutorialRepository {
        /** 읽기 실패는 본 것으로 떨어뜨린다. 매번 다시 뜨는 것보다 한 번 놓치는 편이 낫다. */
        override fun observeSeen(tutorial: Tutorial): Flow<Boolean> =
            dataStore.data
                .map { preferences -> preferences[tutorial.seenKey] == true }
                .catch { error -> if (error is IOException) emit(true) else throw error }
                .distinctUntilChanged()

        override suspend fun markSeen(tutorial: Tutorial) {
            dataStore.edit { preferences -> preferences[tutorial.seenKey] = true }
        }

        override suspend fun resetAll() {
            dataStore.edit { preferences -> Tutorial.entries.forEach { preferences.remove(it.seenKey) } }
        }

        private val Tutorial.seenKey: Preferences.Key<Boolean>
            get() = booleanPreferencesKey("tutorial_seen_${name.lowercase()}")
    }
