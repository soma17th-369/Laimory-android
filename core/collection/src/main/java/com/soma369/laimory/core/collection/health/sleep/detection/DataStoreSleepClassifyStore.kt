package com.soma369.laimory.core.collection.health.sleep.detection

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SleepClassifyStore] 의 DataStore 구현.
 *
 * 최근 [MAX_SAMPLES] 개만 링버퍼처럼 유지한다(밤 한 번을 덮고도 남는 크기). 저장 포맷은 `ts:conf` 를 `;` 로 이은 CSV.
 */
@Singleton
internal class DataStoreSleepClassifyStore
    @Inject
    constructor(
        @SleepClassifyDataStore private val dataStore: DataStore<Preferences>,
    ) : SleepClassifyStore {
        override suspend fun add(samples: List<SleepClassifySample>) {
            if (samples.isEmpty()) return
            dataStore.edit { prefs ->
                val merged =
                    (parse(prefs[KEY_SAMPLES]) + samples)
                        .sortedBy { it.timestampMillis }
                        .takeLast(MAX_SAMPLES)
                prefs[KEY_SAMPLES] = serialize(merged)
            }
        }

        override suspend fun all(): List<SleepClassifySample> =
            dataStore.data
                // 디스크 읽기는 DataStore 가 IO 로 처리하고, CSV 파싱(CPU) 은 flowOn 으로 Default 에 고정한다.
                .map { prefs -> parse(prefs[KEY_SAMPLES]) }
                .flowOn(Dispatchers.Default)
                .first()

        private companion object {
            val KEY_SAMPLES = stringPreferencesKey("classify_samples")
            const val MAX_SAMPLES = 400

            fun parse(raw: String?): List<SleepClassifySample> =
                raw
                    ?.split(';')
                    ?.filter { it.isNotBlank() }
                    ?.mapNotNull { entry ->
                        val parts = entry.split(':')
                        val ts = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
                        val confidence = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                        SleepClassifySample(ts, confidence)
                    }.orEmpty()

            fun serialize(samples: List<SleepClassifySample>): String =
                samples.joinToString(";") { "${it.timestampMillis}:${it.confidence}" }
        }
    }
