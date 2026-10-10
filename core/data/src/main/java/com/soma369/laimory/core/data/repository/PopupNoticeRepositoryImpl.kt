package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.soma369.laimory.core.data.datasource.remote.AppInitializerRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.NoticeRemoteDataSource
import com.soma369.laimory.core.data.di.AppSettingsDataStore
import com.soma369.laimory.core.data.model.notice.toDomain
import com.soma369.laimory.core.data.model.onboarding.toDomain
import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.domain.model.notice.PopupNotice
import com.soma369.laimory.core.domain.repository.PopupNoticeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 띄운 기록은 공지 읽음과 같은 앱 설정 저장소에 둔다 — 로그아웃해도 남는 기기 단위 값이다.
 *
 * Room 이 아닌 이유: 저장할 것은 id 몇 개짜리 집합이고 묻는 것은 "봤나?" 하나뿐이다.
 */
internal class PopupNoticeRepositoryImpl
    @Inject
    constructor(
        private val initializerRemoteDataSource: AppInitializerRemoteDataSource,
        private val noticeRemoteDataSource: NoticeRemoteDataSource,
        @AppSettingsDataStore private val dataStore: DataStore<Preferences>,
    ) : PopupNoticeRepository {
        override suspend fun getPopupNotices(): List<PopupNotice> = initializerRemoteDataSource.fetch().popupNotices.map { it.toDomain() }

        override suspend fun getNotice(noticeId: Long): Notice =
            noticeRemoteDataSource.getNotice(noticeId).toDomain()
                ?: throw IllegalStateException("게시 시각을 읽지 못한 공지")

        override suspend fun getSeenIds(): Set<Long> = dataStore.data.first().seenIds()

        override suspend fun markSeen(noticeId: Long) {
            dataStore.edit { preferences ->
                val kept = (preferences.seenIds() + noticeId).sortedDescending().take(MAX_SEEN_IDS)
                preferences[KEY_SEEN_POPUP_NOTICE_IDS] = kept.mapTo(mutableSetOf(), Long::toString)
            }
        }

        private fun Preferences.seenIds(): Set<Long> =
            this[KEY_SEEN_POPUP_NOTICE_IDS].orEmpty().mapNotNullTo(mutableSetOf(), String::toLongOrNull)

        private companion object {
            val KEY_SEEN_POPUP_NOTICE_IDS = stringSetPreferencesKey("seen_popup_notice_ids")

            /**
             * 남겨 둘 개수. 서버 목록에서 빠진 id 를 지우지 않으므로(숨겼다 재노출하면 같은 id 로 돌아온다) 상한으로만
             * 줄인다. 공지 id 는 커지는 순이라 큰 것부터 남기면 최근 것이 남는다. 200개라도 수 KB 다.
             */
            const val MAX_SEEN_IDS = 200
        }
    }
