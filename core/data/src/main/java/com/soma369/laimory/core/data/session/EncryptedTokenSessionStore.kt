package com.soma369.laimory.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.data.di.AuthSessionDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** access/refresh 쌍을 하나의 암호화 값으로 저장해 회전 중 부분 갱신을 방지한다. */
@Singleton
internal class EncryptedTokenSessionStore
    @Inject
    constructor(
        @AuthSessionDataStore private val dataStore: DataStore<Preferences>,
        private val cipher: SessionCipher,
        private val json: Json,
    ) : TokenSessionStore {
        override fun observe(): Flow<TokenSession?> =
            dataStore.data
                .catch { error ->
                    if (error is IOException) emit(emptyPreferences()) else throw error
                }.map { preferences ->
                    preferences[KEY_SESSION]?.let(::decodeOrNull)
                }

        override suspend fun get(): TokenSession? = observe().first()

        override suspend fun save(session: TokenSession) {
            val plainText = json.encodeToString(session).encodeToByteArray()
            val encrypted = cipher.encrypt(plainText)
            dataStore.edit { preferences -> preferences[KEY_SESSION] = encrypted }
        }

        override suspend fun clear() {
            dataStore.edit { preferences -> preferences.remove(KEY_SESSION) }
        }

        private fun decodeOrNull(encrypted: String): TokenSession? =
            runCatching {
                val plainText = cipher.decrypt(encrypted).decodeToString()
                json.decodeFromString<TokenSession>(plainText)
            }.getOrNull()

        private companion object {
            val KEY_SESSION = stringPreferencesKey("encrypted_session")
        }
    }
