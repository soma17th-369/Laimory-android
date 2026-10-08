package com.soma369.laimory.notice

import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.domain.repository.NoticeRepository
import com.soma369.laimory.core.domain.repository.PopupNoticeRepository
import com.soma369.laimory.core.domain.usecase.notice.GetPopupNoticesUseCase
import com.soma369.laimory.core.domain.usecase.notice.MarkPopupNoticeSeenUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `최신 것부터 하나씩 띄우고 닫으면 다음으로 넘어간다`() =
        runTest {
            popupRepository.ids = listOf(12, 9)

            queue.loadOnce()
            assertEquals(12L, queue.current.value?.id)

            queue.close(queue.current.value!!, opened = false)
            assertEquals(9L, queue.current.value?.id)

            queue.close(queue.current.value!!, opened = true)
            assertNull(queue.current.value)
            assertEquals(setOf(12L, 9L), popupRepository.seen)
            assertEquals(setOf(9L), noticeRepository.read)
        }

    @Test
    fun `같은 프로세스에서는 한 번만 받는다`() =
        runTest {
            popupRepository.ids = listOf(12)

            queue.loadOnce()
            queue.loadOnce()

            assertEquals(1, popupRepository.idsCallCount)
        }

    @Test
    fun `지금 띄운 것이 아닌 팝업을 닫으라는 요청은 무시한다`() =
        runTest {
            // 닫기를 두 번 누르면 두 번째 요청은 이미 넘어간 다음 팝업을 닫으면 안 된다.
            popupRepository.ids = listOf(12, 9)
            queue.loadOnce()
            val first = queue.current.value!!
            queue.close(first, opened = false)

            queue.close(first, opened = false)

            assertEquals(9L, queue.current.value?.id)
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
