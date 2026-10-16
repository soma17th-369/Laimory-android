package com.soma369.laimory.notice

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.domain.model.notice.PopupNotice
import com.soma369.laimory.core.domain.repository.NoticeRepository
import com.soma369.laimory.core.domain.repository.PopupNoticeRepository
import com.soma369.laimory.core.domain.usecase.notice.GetPopupNoticeContentUrlUseCase
import com.soma369.laimory.core.domain.usecase.notice.GetPopupNoticesUseCase
import com.soma369.laimory.core.domain.usecase.notice.MarkPopupNoticeOpenedUseCase
import com.soma369.laimory.core.domain.usecase.notice.MarkPopupNoticeSeenUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PopupNoticeQueueTest {
    private val popupRepository = FakePopupNoticeRepository()
    private val noticeRepository = FakeNoticeRepository()
    private val queue =
        PopupNoticeQueue(
            getPopupNotices = GetPopupNoticesUseCase(popupRepository),
            getContentUrl = GetPopupNoticeContentUrlUseCase(popupRepository),
            markPopupNoticeSeen = MarkPopupNoticeSeenUseCase(popupRepository),
            markPopupNoticeOpened = MarkPopupNoticeOpenedUseCase(noticeRepository),
        )

    @Test
    fun `다시 보지 않기 한 것을 빼고 최신 순 목록으로 내놓는다`() =
        runTest {
            popupRepository.popups = listOf(popup(12), popup(9), popup(5))
            popupRepository.seen += 9

            queue.refresh()

            assertEquals(listOf(12L, 5L), queue.ids())
        }

    @Test
    fun `카드를 처리하면 대기열에서 빠져 남은 첫 장이 다음 차례다`() =
        runTest {
            popupRepository.popups = listOf(popup(12), popup(9), popup(5))
            queue.refresh()

            queue.close(popup(12))
            assertEquals(listOf(9L, 5L), queue.ids())

            // 점을 눌러 가운데가 아닌 카드를 처리해도 그 카드만 빠진다.
            queue.dismissForever(popup(5))
            assertEquals(listOf(9L), queue.ids())
        }

    @Test
    fun `다시 보지 않기만 기기에 남고 닫기는 남지 않는다`() =
        runTest {
            popupRepository.popups = listOf(popup(12), popup(9))
            queue.refresh()

            queue.dismissForever(popup(12))
            queue.close(popup(9))

            assertEquals(setOf(12L), popupRepository.seen)
            assertTrue(noticeRepository.read.isEmpty())
        }

    @Test
    fun `닫은 공지는 앱 안에서 홈에 돌아와도 다시 뜨지 않는다`() =
        runTest {
            popupRepository.popups = listOf(popup(12), popup(9))
            queue.refresh()
            queue.close(popup(12))
            queue.close(popup(9))

            // 그사이 새 공지가 올라왔다. 홈 진입 확인은 새 공지만 잡는다.
            popupRepository.popups = listOf(popup(15), popup(12), popup(9))
            queue.refresh()

            assertEquals(listOf(15L), queue.ids())
        }

    @Test
    fun `앱을 나갔다 다시 열면 닫은 공지가 다시 뜨고 다시 보지 않기 한 것은 안 뜬다`() =
        runTest {
            popupRepository.popups = listOf(popup(12), popup(9))
            queue.refresh()
            queue.dismissForever(popup(12))
            queue.close(popup(9))

            queue.onStop(ProcessOwner)
            queue.refresh()

            assertEquals(listOf(9L), queue.ids())
        }

    @Test
    fun `시트를 내리면 남은 카드를 모두 닫는다`() =
        runTest {
            popupRepository.popups = listOf(popup(12), popup(9), popup(5))
            queue.refresh()
            queue.dismissForever(popup(12))

            queue.closeAll()
            assertTrue(queue.notices.value.isEmpty())
            assertEquals(setOf(12L), popupRepository.seen)

            queue.refresh()
            assertTrue(queue.notices.value.isEmpty())

            queue.onStop(ProcessOwner)
            queue.refresh()
            assertEquals(listOf(9L, 5L), queue.ids())
        }

    @Test
    fun `시트가 떠 있으면 다시 받지 않는다`() =
        runTest {
            popupRepository.popups = listOf(popup(12))
            queue.refresh()

            popupRepository.popups = listOf(popup(15), popup(12))
            queue.refresh()

            assertEquals(1, popupRepository.popupsCallCount)
            assertEquals(listOf(12L), queue.ids())
        }

    @Test
    fun `받지 못했으면 다음 홈 진입에서 다시 받는다`() =
        runTest {
            popupRepository.popupsFailure = IllegalStateException("502")
            queue.refresh()
            assertTrue(queue.notices.value.isEmpty())

            popupRepository.popupsFailure = null
            popupRepository.popups = listOf(popup(12))
            queue.refresh()

            assertEquals(listOf(12L), queue.ids())
        }

    @Test
    fun `원문을 열면 카드는 그대로 두고 공지 읽음에만 남긴다`() =
        runTest {
            popupRepository.popups = listOf(popup(12), popup(9))
            queue.refresh()

            queue.markOpened(popup(12))

            assertEquals(listOf(12L, 9L), queue.ids())
            assertTrue(popupRepository.seen.isEmpty())
            assertEquals(setOf(12L), noticeRepository.read)
        }

    @Test
    fun `원문 주소는 누를 때 단건 조회로 받고 숨겨졌으면 없다`() =
        runTest {
            popupRepository.popups = listOf(popup(12), popup(9))
            popupRepository.missing += 9
            queue.refresh()

            assertEquals("https://www.laimory.app/notices/12", queue.contentUrlOf(popup(12)))
            assertNull(queue.contentUrlOf(popup(9)))
        }

    private fun PopupNoticeQueue.ids(): List<Long> = notices.value.map(PopupNotice::id)

    private class FakePopupNoticeRepository : PopupNoticeRepository {
        var popups: List<PopupNotice> = emptyList()
        var popupsFailure: Throwable? = null
        var popupsCallCount = 0
        val seen = mutableSetOf<Long>()
        val missing = mutableSetOf<Long>()

        override suspend fun getPopupNotices(): List<PopupNotice> {
            popupsCallCount++
            return popupsFailure?.let { throw it } ?: popups
        }

        override suspend fun getNotice(noticeId: Long): Notice {
            if (noticeId in missing) throw ApiException.ClientException(rawCode = 404, errorCode = -404)
            return Notice(noticeId, "공지 $noticeId", "https://www.laimory.app/notices/$noticeId", LocalDateTime.of(2026, 10, 8, 10, 0))
        }

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

    /** 앱 프로세스 수명 자리. 대기열은 소유자를 읽지 않는다. */
    private object ProcessOwner : LifecycleOwner {
        override val lifecycle: Lifecycle get() = error("읽지 않는다")
    }

    private companion object {
        fun popup(id: Long) = PopupNotice(id, "공지 $id", "https://cdn.laimory.app/notices/$id.webp")
    }
}
