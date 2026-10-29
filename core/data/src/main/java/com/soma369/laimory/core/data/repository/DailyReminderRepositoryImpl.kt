package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.soma369.laimory.core.data.di.AppSettingsDataStore
import com.soma369.laimory.core.domain.model.reminder.DailyReminderDeliveryMemory
import com.soma369.laimory.core.domain.model.reminder.DailyReminderSettings
import com.soma369.laimory.core.domain.repository.DailyReminderRepository
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * 일일 리마인더 상태를 앱 설정 저장소에 둔다. 화면 모드처럼 이 기기를 쓰는 사람이 정한 값이라 로그아웃해도 남는다.
 *
 * 이 파일은 Auto Backup·기기 이전 대상이다. 백업이 있는 재설치에서는 켜짐 값이 먼저 돌아오므로, 서버의 예전
 * 선택(전환 뒤에는 모두 꺼짐)을 다시 이어받지 않는다.
 *
 * 시각은 자정부터의 분, 날짜는 epoch day 로 적는다.
 */
internal class DailyReminderRepositoryImpl
    @Inject
    constructor(
        @AppSettingsDataStore private val dataStore: DataStore<Preferences>,
    ) : DailyReminderRepository {
        private val preferences: Flow<Preferences> =
            dataStore.data.catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

        override val settings: Flow<DailyReminderSettings> =
            preferences
                .map { preferences ->
                    DailyReminderSettings(
                        isEnabled = preferences[KEY_ENABLED] ?: DailyReminderSettings.DEFAULT.isEnabled,
                        time = preferences[KEY_TIME_MINUTE]?.toLocalTimeOrNull() ?: DailyReminderSettings.DEFAULT_TIME,
                    )
                }.distinctUntilChanged()

        override val isServerReminderHandedOff: Flow<Boolean> =
            preferences.map { preferences -> preferences[KEY_SERVER_HANDED_OFF] == true }.distinctUntilChanged()

        override val pushEnabledMirror: Flow<Boolean?> =
            preferences.map { preferences -> preferences[KEY_PUSH_ENABLED_MIRROR] }.distinctUntilChanged()

        override suspend fun hasStoredEnabled(): Boolean = preferences.first()[KEY_ENABLED] != null

        override suspend fun setEnabled(isEnabled: Boolean) {
            dataStore.edit { preferences -> preferences[KEY_ENABLED] = isEnabled }
        }

        override suspend fun setTime(time: LocalTime) {
            dataStore.edit { preferences -> preferences[KEY_TIME_MINUTE] = time.toSecondOfDay() / SECONDS_PER_MINUTE }
        }

        override suspend fun markServerReminderHandedOff() {
            dataStore.edit { preferences -> preferences[KEY_SERVER_HANDED_OFF] = true }
        }

        override suspend fun setPushEnabledMirror(isEnabled: Boolean) {
            editQuietly("전체 알림 거울값") { preferences -> preferences[KEY_PUSH_ENABLED_MIRROR] = isEnabled }
        }

        override suspend fun deliveryMemory(): DailyReminderDeliveryMemory {
            val preferences = preferences.first()
            return DailyReminderDeliveryMemory(
                lastSavedRecordDate = preferences[KEY_LAST_SAVED_EPOCH_DAY]?.let(LocalDate::ofEpochDay),
                lastGeneralTitleIndex = preferences[KEY_LAST_GENERAL_TITLE_INDEX],
                lastCompletedCopyIndex = preferences[KEY_LAST_COMPLETED_COPY_INDEX],
                lastDeliveredRecordDate = preferences[KEY_LAST_DELIVERED_RECORD_EPOCH_DAY]?.let(LocalDate::ofEpochDay),
            )
        }

        override suspend fun markRecordSaved(recordDate: LocalDate) {
            editQuietly("완료 기록 날짜") { preferences -> preferences[KEY_LAST_SAVED_EPOCH_DAY] = recordDate.toEpochDay() }
        }

        override suspend fun setLastGeneralTitleIndex(index: Int) {
            editQuietly("일반 문구 기억") { preferences -> preferences[KEY_LAST_GENERAL_TITLE_INDEX] = index }
        }

        override suspend fun setLastCompletedCopyIndex(index: Int) {
            editQuietly("완료 문구 기억") { preferences -> preferences[KEY_LAST_COMPLETED_COPY_INDEX] = index }
        }

        override suspend fun setLastDeliveredRecordDate(date: LocalDate) {
            editQuietly("마지막 알림 날짜") { preferences -> preferences[KEY_LAST_DELIVERED_RECORD_EPOCH_DAY] = date.toEpochDay() }
        }

        /** 보조 기록은 실패해도 호출부의 본래 동작(저장·조회)을 막지 않는다. */
        private suspend fun editQuietly(
            label: String,
            transform: (MutablePreferences) -> Unit,
        ) {
            try {
                dataStore.edit(transform)
            } catch (error: IOException) {
                Logger.w(LogDomain.REPOSITORY, "일일 리마인더 $label 저장 실패: ${error::class.simpleName}")
            }
        }

        private fun Int.toLocalTimeOrNull(): LocalTime? {
            if (this !in 0 until MINUTES_PER_DAY) return null
            return LocalTime.ofSecondOfDay(toLong() * SECONDS_PER_MINUTE)
        }

        private companion object {
            val KEY_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
            val KEY_TIME_MINUTE = intPreferencesKey("daily_reminder_time_minute")
            val KEY_SERVER_HANDED_OFF = booleanPreferencesKey("daily_reminder_server_handed_off")
            val KEY_PUSH_ENABLED_MIRROR = booleanPreferencesKey("daily_reminder_push_enabled_mirror")
            val KEY_LAST_SAVED_EPOCH_DAY = longPreferencesKey("daily_reminder_last_saved_epoch_day")
            val KEY_LAST_GENERAL_TITLE_INDEX = intPreferencesKey("daily_reminder_last_general_title_index")
            val KEY_LAST_COMPLETED_COPY_INDEX = intPreferencesKey("daily_reminder_last_completed_copy_index")
            val KEY_LAST_DELIVERED_RECORD_EPOCH_DAY = longPreferencesKey("daily_reminder_last_delivered_record_epoch_day")
            const val SECONDS_PER_MINUTE = 60
            const val MINUTES_PER_DAY = 24 * 60
        }
    }
