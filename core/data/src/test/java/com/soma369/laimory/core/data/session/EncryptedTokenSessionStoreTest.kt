package com.soma369.laimory.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class EncryptedTokenSessionStoreTest {
    private val dataStore = InMemoryPreferencesDataStore()
    private val cipher = FakeSessionCipher()
    private val store = EncryptedTokenSessionStore(dataStore, cipher, Json)

    @Test
    fun `token 쌍을 하나의 암호화 preference로 저장하고 복원한다`() =
        runTest {
            store.save(TokenSession("access-secret", "refresh-secret"))

            val restored = store.get()
            val persistedValue = dataStore.current.asMap().values.single() as String

            assertEquals("access-secret", restored?.accessToken)
            assertEquals("refresh-secret", restored?.refreshToken)
            assertFalse(persistedValue.contains("access-secret"))
            assertFalse(persistedValue.contains("refresh-secret"))
        }

    @Test
    fun `세션 삭제는 token 쌍을 함께 제거한다`() =
        runTest {
            store.save(TokenSession("access", "refresh"))

            store.clear()

            assertNull(store.get())
            assertEquals(0, dataStore.current.asMap().size)
        }

    @Test
    fun `복호화할 수 없는 세션은 미인증으로 취급한다`() =
        runTest {
            store.save(TokenSession("access", "refresh"))
            cipher.rejectDecryption = true

            assertNull(store.get())
        }

    private class FakeSessionCipher : SessionCipher {
        var rejectDecryption = false

        override fun encrypt(plainText: ByteArray): String = "encrypted:" + Base64.getEncoder().encodeToString(plainText)

        override fun decrypt(cipherText: String): ByteArray {
            check(!rejectDecryption)
            return Base64.getDecoder().decode(cipherText.removePrefix("encrypted:"))
        }
    }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())
        private val mutex = Mutex()

        val current: Preferences get() = state.value

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock {
                transform(state.value).also { state.value = it }
            }
    }
}
