package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.soma369.laimory.core.data.datasource.remote.NoticeRemoteDataSource
import com.soma369.laimory.core.data.di.AppSettingsDataStore
import com.soma369.laimory.core.data.model.notice.toDomain
import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.domain.repository.NoticeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 읽음 기록은 앱 설정 저장소에 둔다 — 로그아웃해도 남는 기기 단위 값이다. 공지는 계정과 무관한
 * 공개 글이라, 다른 계정으로 들어와도 이 기기에서 이미 읽은 공지를 다시 새 공지로 띄울 이유가 없다.
 */
internal class NoticeRepositoryImpl
    @Inject
    constructor(
        private val remoteDataSource: NoticeRemoteDataSource,
        @AppSettingsDataStore private val dataStore: DataStore<Preferences>,
    ) : NoticeRepository {
        override suspend fun getNotices(): List<Notice> = remoteDataSource.getNotices().notices.mapNotNull { it.toDomain() }

        override suspend fun getReadNoticeIds(): Set<Long> = dataStore.data.first().readIds()

        override suspend fun markRead(
            noticeId: Long,
            keepIds: Set<Long>,
        ) {
            dataStore.edit { preferences ->
                val kept = preferences.readIds().filterTo(mutableSetOf()) { it in keepIds }
                preferences[KEY_READ_NOTICE_IDS] = (kept + noticeId).mapTo(mutableSetOf(), Long::toString)
            }
        }

        private fun Preferences.readIds(): Set<Long> =
            this[KEY_READ_NOTICE_IDS].orEmpty().mapNotNullTo(mutableSetOf(), String::toLongOrNull)

        private companion object {
            val KEY_READ_NOTICE_IDS = stringSetPreferencesKey("read_notice_ids")
        }
    }
