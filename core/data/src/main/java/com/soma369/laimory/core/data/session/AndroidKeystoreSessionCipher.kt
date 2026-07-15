package com.soma369.laimory.core.data.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Android Keystore의 non-exportable AES-GCM 키로 인증 세션 blob을 암·복호화한다. */
@Singleton
internal class AndroidKeystoreSessionCipher
    @Inject
    constructor() : SessionCipher {
        override fun encrypt(plainText: ByteArray): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(), SecureRandom())
            val encrypted = cipher.doFinal(plainText)
            val payload =
                ByteBuffer.allocate(1 + cipher.iv.size + encrypted.size)
                    .put(cipher.iv.size.toByte())
                    .put(cipher.iv)
                    .put(encrypted)
                    .array()
            return Base64.getEncoder().encodeToString(payload)
        }

        override fun decrypt(cipherText: String): ByteArray {
            val payload = Base64.getDecoder().decode(cipherText)
            require(payload.isNotEmpty()) { "Empty auth session payload" }

            val buffer = ByteBuffer.wrap(payload)
            val ivSize = buffer.get().toInt() and 0xFF
            require(ivSize in MIN_IV_SIZE..MAX_IV_SIZE && buffer.remaining() > ivSize) {
                "Invalid auth session payload"
            }
            val iv = ByteArray(ivSize).also(buffer::get)
            val encrypted = ByteArray(buffer.remaining()).also(buffer::get)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            return cipher.doFinal(encrypted)
        }

        private fun getOrCreateKey(): SecretKey =
            synchronized(KEY_LOCK) {
                val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
                (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: createKey()
            }

        private fun createKey(): SecretKey {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            return generator.generateKey()
        }

        private companion object {
            const val KEYSTORE_PROVIDER = "AndroidKeyStore"
            const val KEY_ALIAS = "laimory_auth_session_v1"
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val GCM_TAG_BITS = 128
            const val MIN_IV_SIZE = 12
            const val MAX_IV_SIZE = 32
            val KEY_LOCK = Any()
        }
    }
