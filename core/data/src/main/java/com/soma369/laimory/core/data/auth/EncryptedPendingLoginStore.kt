package com.soma369.laimory.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.data.di.AuthSessionDataStore
import com.soma369.laimory.core.data.session.SessionCipher
import javax.inject.Inject
import javax.inject.Singleton

/** verifier를 인증 세션과 같은 Keystore AES-GCM 키로 암호화해 DataStore에 저장한다. */
@Singleton
internal class EncryptedPendingLoginStore
    @Inject
    constructor(
        @AuthSessionDataStore private val dataStore: DataStore<Preferences>,
        private val cipher: SessionCipher,
    ) : PendingLoginStore {
        override suspend fun save(verifier: String) {
            val encrypted = cipher.encrypt(verifier.encodeToByteArray())
            dataStore.edit { preferences -> preferences[KEY_PENDING_VERIFIER] = encrypted }
        }

        override suspend fun consume(): String? {
            var encrypted: String? = null
            dataStore.edit { preferences ->
                encrypted = preferences[KEY_PENDING_VERIFIER]
                preferences.remove(KEY_PENDING_VERIFIER)
            }
            return encrypted?.let(::decryptOrNull)
        }

        override suspend fun clear() {
            dataStore.edit { preferences -> preferences.remove(KEY_PENDING_VERIFIER) }
        }

        private fun decryptOrNull(encrypted: String): String? =
            runCatching { cipher.decrypt(encrypted).decodeToString() }
                .getOrNull()

        private companion object {
            val KEY_PENDING_VERIFIER = stringPreferencesKey("encrypted_pending_login_verifier")
        }
    }
