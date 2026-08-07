package com.soma369.laimory.core.data.helper

import com.soma369.laimory.core.domain.helper.GlobalLoadingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [GlobalLoadingHelper] 구현체.
 *
 * key별 실행 횟수를 세어, 같은 key가 겹치거나 서로 다른 key가 남아 있는 동안 로딩을 유지한다.
 * token 해제는 [withLoading]의 finally가 보장한다.
 */
@Singleton
class GlobalLoadingHelperImpl
    @Inject
    constructor() : GlobalLoadingHelper {
        private val lock = Any()
        private val activeCounts = mutableMapOf<String, Int>()

        private val _activeKeys = MutableStateFlow<Set<String>>(emptySet())
        override val activeKeys: StateFlow<Set<String>> = _activeKeys.asStateFlow()

        override suspend fun <T> withLoading(
            key: String,
            block: suspend () -> T,
        ): T {
            acquire(key)
            return try {
                block()
            } finally {
                release(key)
            }
        }

        private fun acquire(key: String) {
            synchronized(lock) {
                activeCounts[key] = (activeCounts[key] ?: 0) + 1
                _activeKeys.value = activeCounts.keys.toSet()
            }
        }

        private fun release(key: String) {
            synchronized(lock) {
                val remaining = (activeCounts[key] ?: 0) - 1
                if (remaining <= 0) activeCounts.remove(key) else activeCounts[key] = remaining
                _activeKeys.value = activeCounts.keys.toSet()
            }
        }
    }
