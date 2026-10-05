package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.soma369.laimory.core.data.di.AppSettingsDataStore
import com.soma369.laimory.core.domain.model.settings.DefaultRecordRange
import com.soma369.laimory.core.domain.repository.DefaultRecordRangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime
import javax.inject.Inject

/**
 * 기본 기록 범위를 앱 설정 저장소에 둔다. 화면 모드처럼 이 기기를 쓰는 사람이 정한 값이라 로그아웃해도 남는다.
 *
 * 시각은 자정부터의 분으로 적는다. 세 키 중 하나라도 없거나 시각으로 읽을 수 없으면 저장한 적이 없는 것으로
 * 본다 — 반쯤 남은 값으로 범위를 만들면 사용자가 고른 적 없는 범위가 된다.
 */
internal class DefaultRecordRangeRepositoryImpl
    @Inject
    constructor(
        @AppSettingsDataStore private val dataStore: DataStore<Preferences>,
    ) : DefaultRecordRangeRepository {
        /**
         * 읽기 실패는 처음 값으로 떨어뜨린다. 이 흐름이 홈의 첫 범위를 정하므로, 예외를 올리면 홈이 범위를 정하지 못한다.
         *
         * 같은 파일의 다른 설정(화면 모드 등)이 바뀔 때도 방출되므로 같은 값은 거른다.
         */
        override val range: Flow<DefaultRecordRange> =
            dataStore.data
                .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                .map { preferences -> preferences.toRange() ?: DefaultRecordRange.INITIAL }
                .distinctUntilChanged()

        override suspend fun setRange(range: DefaultRecordRange) {
            dataStore.edit { preferences ->
                preferences[KEY_START_MINUTE] = range.startTime.minuteOfDay()
                preferences[KEY_ENDS_NEXT_DAY] = range.endsNextDay
                preferences[KEY_END_MINUTE] = range.endTime.minuteOfDay()
            }
        }

        private fun Preferences.toRange(): DefaultRecordRange? {
            val start = this[KEY_START_MINUTE]?.toLocalTimeOrNull() ?: return null
            val endsNextDay = this[KEY_ENDS_NEXT_DAY] ?: return null
            val end = this[KEY_END_MINUTE]?.toLocalTimeOrNull() ?: return null
            return DefaultRecordRange(startTime = start, endsNextDay = endsNextDay, endTime = end)
        }

        private fun LocalTime.minuteOfDay(): Int = toSecondOfDay() / SECONDS_PER_MINUTE

        private fun Int.toLocalTimeOrNull(): LocalTime? {
            if (this !in 0 until MINUTES_PER_DAY) return null
            return LocalTime.ofSecondOfDay(toLong() * SECONDS_PER_MINUTE)
        }

        private companion object {
            val KEY_START_MINUTE = intPreferencesKey("default_record_range_start_minute")
            val KEY_ENDS_NEXT_DAY = booleanPreferencesKey("default_record_range_ends_next_day")
            val KEY_END_MINUTE = intPreferencesKey("default_record_range_end_minute")
            const val SECONDS_PER_MINUTE = 60
            const val MINUTES_PER_DAY = 24 * 60
        }
    }
