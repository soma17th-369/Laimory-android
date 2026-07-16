package com.soma369.laimory.core.data.auth

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject

/** RFC 7636 S256 규칙에 맞는 43자 verifier/challenge 쌍을 만든다. */
internal class SecurePkceGenerator
    @Inject
    constructor() : PkceGenerator {
        private val secureRandom = SecureRandom()

        override fun generate(): PkcePair {
            val randomBytes = ByteArray(VERIFIER_BYTE_SIZE).also(secureRandom::nextBytes)
            val verifier = URL_ENCODER.encodeToString(randomBytes)
            return PkcePair(
                verifier = verifier,
                challenge = challengeOf(verifier),
            )
        }

        companion object {
            private const val VERIFIER_BYTE_SIZE = 32
            private val URL_ENCODER = Base64.getUrlEncoder().withoutPadding()

            internal fun challengeOf(verifier: String): String {
                val digest =
                    MessageDigest.getInstance("SHA-256")
                        .digest(verifier.toByteArray(StandardCharsets.US_ASCII))
                return URL_ENCODER.encodeToString(digest)
            }
        }
    }
