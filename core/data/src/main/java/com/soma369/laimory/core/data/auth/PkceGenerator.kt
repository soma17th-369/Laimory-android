package com.soma369.laimory.core.data.auth

internal fun interface PkceGenerator {
    fun generate(): PkcePair
}
