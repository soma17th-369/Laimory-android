package com.soma369.laimory.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.soma369.laimory.core.data.session.SessionCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class EncryptedPendingLoginStoreTest {
    private val dataStore = InMemoryPreferencesDataStore()
    private val store = EncryptedPendingLoginStore(dataStore, FakeSessionCipher())

    @Test
    fun `verifier를 암호화해 저장하고 프로세스가 바뀌어도 복원한다`() =
        runTest {
            store.save("secret-verifier")
            val persistedValue = dataStore.current.asMap().values.single().toString()
            val recreatedStore = EncryptedPendingLoginStore(dataStore, FakeSessionCipher())

            val restored = recreatedStore.consume()

            assertEquals("secret-verifier", restored)
            assertFalse(persistedValue.contains("secret-verifier"))
        }

    @Test
    fun `consume은 verifier를 한 번만 반환한다`() =
        runTest {
            store.save("verifier")

            assertEquals("verifier", store.consume())
            assertNull(store.consume())
        }

    @Test
    fun `새 시도는 이전 verifier를 덮어쓴다`() =
        runTest {
            store.save("old")
            store.save("new")

            assertEquals("new", store.consume())
        }

    private class FakeSessionCipher : SessionCipher {
        override fun encrypt(plainText: ByteArray): String = "encrypted:" + Base64.getEncoder().encodeToString(plainText)

        override fun decrypt(cipherText: String): ByteArray = Base64.getDecoder().decode(cipherText.removePrefix("encrypted:"))
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
