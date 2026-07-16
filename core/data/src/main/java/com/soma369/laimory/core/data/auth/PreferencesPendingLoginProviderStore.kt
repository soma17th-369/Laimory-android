package com.soma369.laimory.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.data.di.AuthSessionDataStore
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 민감정보가 아닌 로그인 제공자를 인증 DataStore에 저장한다. */
@Singleton
internal class PreferencesPendingLoginProviderStore
    @Inject
    constructor(
        @AuthSessionDataStore private val dataStore: DataStore<Preferences>,
    ) : PendingLoginProviderStore {
        override suspend fun save(provider: SocialLoginProvider) {
            dataStore.edit { preferences -> preferences[KEY_PROVIDER] = provider.name }
        }

        override suspend fun get(): SocialLoginProvider? =
            dataStore.data
                .map { preferences -> preferences[KEY_PROVIDER].toProviderOrNull() }
                .first()

        override suspend fun clear() {
            dataStore.edit { preferences -> preferences.remove(KEY_PROVIDER) }
        }

        private fun String?.toProviderOrNull(): SocialLoginProvider? =
            SocialLoginProvider.entries.firstOrNull { provider -> provider.name == this }

        private companion object {
            val KEY_PROVIDER = stringPreferencesKey("pending_login_provider")
        }
    }
