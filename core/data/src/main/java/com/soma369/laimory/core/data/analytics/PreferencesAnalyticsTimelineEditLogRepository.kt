package com.soma369.laimory.core.data.analytics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.soma369.laimory.core.data.di.AnalyticsTimelineEditDataStore
import com.soma369.laimory.core.domain.model.analytics.AnalyticsTimelineEditLog
import com.soma369.laimory.core.domain.repository.AnalyticsTimelineEditLogRepository
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 편집 흔적을 기록 날짜별 id 집합으로 남긴다.
 *
 * 횟수가 아니라 id 를 둔다 — 같은 이벤트를 여러 번 고쳐도 한 건이고, 고친 뒤 지운 이벤트는 완료 순간
 * 목록과 겹치지 않아 "고친 것"에서 저절로 빠진다.
 *
 * 분석이 앱을 막지 않도록 실패는 경고 로그로만 남기고, 읽지 못하면 빈 흔적을 준다.
 */
@Singleton
internal class PreferencesAnalyticsTimelineEditLogRepository
    @Inject
    constructor(
        @AnalyticsTimelineEditDataStore private val dataStore: DataStore<Preferences>,
    ) : AnalyticsTimelineEditLogRepository {
        override suspend fun markEdited(
            recordDate: LocalDate,
            timelineEventId: Long,
        ) {
            add(editedKey(recordDate), timelineEventId)
        }

        override suspend fun markDeletedAi(
            recordDate: LocalDate,
            timelineEventId: Long,
        ) {
            add(deletedAiKey(recordDate), timelineEventId)
        }

        override suspend fun take(recordDate: LocalDate): AnalyticsTimelineEditLog {
            var log = AnalyticsTimelineEditLog.EMPTY
            guarded("편집 흔적 읽기") {
                dataStore.edit { preferences ->
                    log =
                        AnalyticsTimelineEditLog(
                            editedEventIds = preferences[editedKey(recordDate)].toIds(),
                            deletedAiEventIds = preferences[deletedAiKey(recordDate)].toIds(),
                        )
                    preferences.remove(editedKey(recordDate))
                    preferences.remove(deletedAiKey(recordDate))
                }
            }
            return log
        }

        override suspend fun clear() {
            guarded("편집 흔적 비우기") {
                dataStore.edit { preferences -> preferences.clear() }
            }
        }

        private suspend fun add(
            key: Preferences.Key<Set<String>>,
            timelineEventId: Long,
        ) {
            guarded("편집 흔적 저장") {
                dataStore.edit { preferences ->
                    preferences[key] = preferences[key].orEmpty() + timelineEventId.toString()
                }
            }
        }

        private suspend fun guarded(
            action: String,
            block: suspend () -> Unit,
        ) {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Logger.w(LogDomain.ANALYTICS, "$action 실패: ${error::class.simpleName}")
            }
        }

        private fun Set<String>?.toIds(): Set<Long> = orEmpty().mapNotNullTo(mutableSetOf(), String::toLongOrNull)

        private fun editedKey(recordDate: LocalDate) = stringSetPreferencesKey("edited:$recordDate")

        private fun deletedAiKey(recordDate: LocalDate) = stringSetPreferencesKey("deleted_ai:$recordDate")
    }
