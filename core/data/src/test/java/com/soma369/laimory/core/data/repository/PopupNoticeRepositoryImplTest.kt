package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.soma369.laimory.core.data.datasource.remote.AppInitializerRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.NoticeRemoteDataSource
import com.soma369.laimory.core.data.model.notice.NoticeListResponse
import com.soma369.laimory.core.data.model.notice.NoticeResponse
import com.soma369.laimory.core.data.model.onboarding.AppInitializerResponse
import com.soma369.laimory.core.data.model.onboarding.PopupNoticeResponse
import com.soma369.laimory.core.domain.model.notice.PopupNotice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PopupNoticeRepositoryImplTest {
    private val dataStore = InMemoryPreferencesDataStore()

    @Test
    fun `팝업 공지는 앱 초기화 응답 순서 그대로 제목 · 썸네일까지 읽는다`() =
        runTest {
            val repository =
                repository(
                    popupNotices =
                        listOf(
                            PopupNoticeResponse(12, "점검 안내", "https://cdn.laimory.app/notices/a.webp"),
                            PopupNoticeResponse(9, "업데이트", "https://cdn.laimory.app/notices/b.webp"),
                        ),
                )

            assertEquals(
                listOf(
                    PopupNotice(12, "점검 안내", "https://cdn.laimory.app/notices/a.webp"),
                    PopupNotice(9, "업데이트", "https://cdn.laimory.app/notices/b.webp"),
                ),
                repository.getPopupNotices(),
            )
        }

    @Test
    fun `팝업 필드가 없는 초기화 응답은 빈 목록으로 읽는다`() {
        // 운영 서버가 이 필드를 싣기 전의 응답이다. 기본값이 없으면 응답 전체를 읽지 못해 온보딩 판정까지 깨진다.
        val json = Json { ignoreUnknownKeys = true }

        val response = json.decodeFromString(AppInitializerResponse.serializer(), """{"onboardingCompleted":true}""")

        assertTrue(response.popupNotices.isEmpty())
    }

    @Test
    fun `이전 계약의 popupNoticeIds 만 있는 응답도 깨지지 않는다`() {
        val json = Json { ignoreUnknownKeys = true }

        val response =
            json.decodeFromString(AppInitializerResponse.serializer(), """{"onboardingCompleted":true,"popupNoticeIds":[3]}""")

        assertTrue(response.popupNotices.isEmpty())
    }

    @Test
    fun `띄운 기록은 남고 서버 목록에서 빠져도 지우지 않는다`() =
        runTest {
            val repository = repository(popupNotices = emptyList())

            repository.markSeen(12)
            repository.markSeen(9)

            assertEquals(setOf(12L, 9L), repository.getSeenIds())
        }

    @Test
    fun `띄운 기록은 id 가 큰 순으로 200개만 남긴다`() =
        runTest {
            val repository = repository(popupNotices = emptyList())

            (1L..201L).forEach { repository.markSeen(it) }

            val seen = repository.getSeenIds()
            assertEquals(200, seen.size)
            assertTrue(1L !in seen)
            assertTrue(201L in seen)
        }

    private fun repository(popupNotices: List<PopupNoticeResponse>) =
        PopupNoticeRepositoryImpl(
            initializerRemoteDataSource =
                object : AppInitializerRemoteDataSource {
                    override suspend fun fetch() = AppInitializerResponse(onboardingCompleted = true, popupNotices = popupNotices)
                },
            noticeRemoteDataSource =
                object : NoticeRemoteDataSource {
                    override suspend fun getNotices() = NoticeListResponse(emptyList())

                    override suspend fun getNotice(noticeId: Long) =
                        NoticeResponse(noticeId, "공지 $noticeId", "https://www.laimory.app/notices/$noticeId", "2026-10-08T10:00:00")
                },
            dataStore = dataStore,
        )

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())
        private val mutex = Mutex()

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock {
                transform(state.value).also { state.value = it }
            }
    }
}
