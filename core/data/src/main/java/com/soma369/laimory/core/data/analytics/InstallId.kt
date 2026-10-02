package com.soma369.laimory.core.data.analytics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.data.di.AnalyticsInstallAttributionDataStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** 설치 구분 값이 저장되는 자리. 설치 유입 귀속과 같은 값을 쓴다. */
internal val INSTALL_ID_KEY = stringPreferencesKey("install_id")

/**
 * 이 설치를 가르는 무작위 값.
 *
 * **자동 백업에서 제외된 저장소**에 둔다 — 재설치나 새 기기 복원으로 되살아나면 설치를 가르는 값이
 * 아니게 된다. 사람도 기기도 가리키지 않고, 기기 안에서만 쓰며 전송하지 않는다.
 */
internal interface InstallIdProvider {
    suspend fun get(): String
}

@Singleton
internal class PreferencesInstallIdProvider
    @Inject
    constructor(
        @AnalyticsInstallAttributionDataStore private val dataStore: DataStore<Preferences>,
    ) : InstallIdProvider {
        @Volatile
        private var cached: String? = null

        override suspend fun get(): String {
            cached?.let { return it }
            var installId: String? = null
            // 읽고 없으면 만드는 것을 한 번의 edit 안에서 한다 — 동시에 불려도 값이 둘로 갈리지 않는다.
            dataStore.edit { preferences ->
                installId = preferences[INSTALL_ID_KEY] ?: UUID.randomUUID().toString().also { preferences[INSTALL_ID_KEY] = it }
            }
            return checkNotNull(installId).also { cached = it }
        }
    }
