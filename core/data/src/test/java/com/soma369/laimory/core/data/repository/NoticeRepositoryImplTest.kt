package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.soma369.laimory.core.data.datasource.remote.NoticeRemoteDataSource
import com.soma369.laimory.core.data.model.notice.NoticeListResponse
import com.soma369.laimory.core.data.model.notice.NoticeResponse
import com.soma369.laimory.core.domain.model.notice.Notice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class NoticeRepositoryImplTest {
    private val dataStore = InMemoryPreferencesDataStore()

    @Test
    fun `서버 정렬 그대로 옮기고 게시 시각을 벽시계로 읽는다`() =
        runTest {
            val remote =
                FakeNoticeRemoteDataSource(
                    listOf(
                        noticeResponse(id = 12, publishedAt = "2026-09-24T10:00:00"),
                        noticeResponse(id = 3, publishedAt = "2026-09-01T09:30:15.123456"),
                    ),
                )

            val notices = repository(remote).getNotices()

            assertEquals(
                listOf(
                    Notice(12, "공지 12", "https://www.laimory.app/notices/12", LocalDateTime.of(2026, 9, 24, 10, 0)),
                    Notice(3, "공지 3", "https://www.laimory.app/notices/3", LocalDateTime.of(2026, 9, 1, 9, 30, 15, 123_456_000)),
                ),
                notices,
            )
        }

    @Test
    fun `게시 시각을 읽지 못한 공지만 버린다`() =
        runTest {
            // 한 건 때문에 목록 전체를 잃으면 멀쩡한 공지까지 못 보여 준다.
            val remote =
                FakeNoticeRemoteDataSource(
                    listOf(
                        noticeResponse(id = 2, publishedAt = "2026-09-24T10:00:00+09:00"),
                        noticeResponse(id = 1, publishedAt = "2026-09-20T10:00:00"),
                    ),
                )

            assertEquals(listOf(1L), repository(remote).getNotices().map { it.id })
        }

    @Test
    fun `공지가 없으면 빈 목록이다`() =
        runTest {
            assertTrue(repository().getNotices().isEmpty())
        }

    @Test
    fun `읽은 공지를 쌓아 두고 돌려준다`() =
        runTest {
            val repository = repository()

            repository.markRead(5, keepIds = setOf(4, 5))
            repository.markRead(4, keepIds = setOf(4, 5))

            assertEquals(setOf(4L, 5L), repository.getReadNoticeIds())
        }

    @Test
    fun `남길 공지 밖의 읽음 기록은 새로 남길 때 버린다`() =
        runTest {
            // 표시 기간이 지난 공지의 읽음 여부는 쓰이지 않는다. 계속 쌓을 이유가 없다.
            val repository = repository()
            repository.markRead(1, keepIds = setOf(1))

            repository.markRead(9, keepIds = setOf(9))

            assertEquals(setOf(9L), repository.getReadNoticeIds())
        }

    @Test
    fun `숫자가 아닌 기록은 건너뛴다`() =
        runTest {
            dataStore.updateData { mutablePreferencesOf(KEY_READ_NOTICE_IDS to setOf("7", "broken")) }

            assertEquals(setOf(7L), repository().getReadNoticeIds())
        }

    private fun repository(
        remote: NoticeRemoteDataSource =
            FakeNoticeRemoteDataSource(
                emptyList(),
            ),
    ) = NoticeRepositoryImpl(remote, dataStore)

    private fun noticeResponse(
        id: Long,
        publishedAt: String,
    ) = NoticeResponse(
        noticeId = id,
        title = "공지 $id",
        contentUrl = "https://www.laimory.app/notices/$id",
        publishedAt = publishedAt,
    )

    private class FakeNoticeRemoteDataSource(
        private val notices: List<NoticeResponse>,
    ) : NoticeRemoteDataSource {
        override suspend fun getNotices(): NoticeListResponse = NoticeListResponse(notices)
    }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())
        private val mutex = Mutex()

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock {
                transform(state.value).also { state.value = it }
            }
    }

    private companion object {
        val KEY_READ_NOTICE_IDS = stringSetPreferencesKey("read_notice_ids")
    }
}
