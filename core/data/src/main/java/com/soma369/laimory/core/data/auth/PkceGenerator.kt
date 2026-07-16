package com.soma369.laimory.core.data.auth

internal data class PkcePair(
    val verifier: String,
    val challenge: String,
) {
    override fun toString(): String = "PkcePair(REDACTED)"
}

internal fun interface PkceGenerator {
    fun generate(): PkcePair
}
