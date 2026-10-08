package com.soma369.laimory.notice

import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.domain.repository.NoticeRepository
import com.soma369.laimory.core.domain.repository.PopupNoticeRepository
import com.soma369.laimory.core.domain.usecase.notice.GetPopupNoticesUseCase
import com.soma369.laimory.core.domain.usecase.notice.MarkPopupNoticeSeenUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PopupNoticeQueueTest {
    private val popupRepository = FakePopupNoticeRepository()
    private val noticeRepository = FakeNoticeRepository()
    private val queue =
        PopupNoticeQueue(
            getPopupNotices = GetPopupNoticesUseCase(popupRepository),
            markPopupNoticeSeen = MarkPopupNoticeSeenUseCase(popupRepository, noticeRepository),
        )

    @Test
    fun `안 본 팝업을 최신 순 목록으로 한 번에 내놓는다`() =
        runTest {
            popupRepository.ids = listOf(12, 9, 5)
            popupRepository.seen += 9

            queue.loadOnce()

            assertEquals(listOf(12L, 5L), queue.notices.value.map(Notice::id))
        }

    @Test
    fun `원문을 열면 시트는 그대로 두고 그 공지만 본 것 · 읽은 것으로 남긴다`() =
        runTest {
            popupRepository.ids = listOf(12, 9)
            queue.loadOnce()

            queue.open(queue.notices.value.first())

            assertEquals(2, queue.notices.value.size)
            assertEquals(setOf(12L), popupRepository.seen)
            assertEquals(setOf(12L), noticeRepository.read)
        }

    @Test
    fun `시트를 닫으면 목록 전부 본 것으로 남기고 읽음은 연 것만이다`() =
        runTest {
            popupRepository.ids = listOf(12, 9)
            queue.loadOnce()
            queue.open(queue.notices.value.first())

            queue.closeAll()

            assertTrue(queue.notices.value.isEmpty())
            assertEquals(setOf(12L, 9L), popupRepository.seen)
            assertEquals(setOf(12L), noticeRepository.read)
        }

    @Test
    fun `같은 프로세스에서는 한 번만 받는다`() =
        runTest {
            popupRepository.ids = listOf(12)

            queue.loadOnce()
            queue.closeAll()
            queue.loadOnce()

            assertEquals(1, popupRepository.idsCallCount)
            assertTrue(queue.notices.value.isEmpty())
        }

    private class FakePopupNoticeRepository : PopupNoticeRepository {
        var ids: List<Long> = emptyList()
        var idsCallCount = 0
        val seen = mutableSetOf<Long>()

        override suspend fun getPopupNoticeIds(): List<Long> {
            idsCallCount++
            return ids
        }

        override suspend fun getNotice(noticeId: Long): Notice =
            Notice(noticeId, "공지 $noticeId", "https://www.laimory.app/notices/$noticeId", LocalDateTime.of(2026, 10, 8, 10, 0))

        override suspend fun getSeenIds(): Set<Long> = seen.toSet()

        override suspend fun markSeen(noticeId: Long) {
            seen += noticeId
        }
    }

    private class FakeNoticeRepository : NoticeRepository {
        val read = mutableSetOf<Long>()

        override suspend fun getNotices(): List<Notice> = emptyList()

        override suspend fun getReadNoticeIds(): Set<Long> = read.toSet()

        override suspend fun markRead(
            noticeId: Long,
            keepIds: Set<Long>,
        ) {
            read.retainAll(keepIds)
            read += noticeId
        }
    }
}
