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
 * "사용자가 위치 수집을 껐다" 는 의사의 영속 저장(단일 소스). 리포지토리·서비스·부팅 리시버가 공유한다.
 *
 * 저장하는 것이 "켰다" 가 아니라 **"껐다"** 인 것이 핵심이다. 켜는 조건은 권한이 말해 주므로
 * 앱이 따로 기억할 필요가 없고, 기억하면 그것을 켜 주는 화면(온보딩)을 보지 않는 기기에서
 * 영영 꺼진 채로 남는다. 여기에는 사용자만 남길 수 있는 것 — 끄겠다는 의사 — 만 둔다.
 *
 * 그래서 실패로 인한 중지는 이 값을 건드리지 않는다. 권한이 돌아오면 다음 전경 진입에서 다시 켠다.
 *
 * 종전 `enabled` 키는 읽지 않는다. 그 값의 `false` 에는 "한 번도 켜진 적 없음" 과 "사용자가 끔" 이
 * 섞여 있는데 전자가 압도적이라, 일괄로 "끄지 않음" 으로 옮기는 편이 낫다 — 알림에서 껐던
 * 사용자는 수집이 한 번 되살아나고, 그때는 설정에서 끌 수 있다.
 */
@Singleton
internal class LocationTrackingPreferences
    @Inject
    constructor(
        @LocationTrackingDataStore private val dataStore: DataStore<Preferences>,
    ) {
        fun observeUserDisabled(): Flow<Boolean> = dataStore.data.map { prefs -> prefs[KEY_USER_DISABLED] ?: false }

        suspend fun isUserDisabled(): Boolean = observeUserDisabled().first()

        suspend fun setUserDisabled(disabled: Boolean) {
            dataStore.edit { prefs -> prefs[KEY_USER_DISABLED] = disabled }
        }

        private companion object {
            val KEY_USER_DISABLED = booleanPreferencesKey("user_disabled")
        }
    }
