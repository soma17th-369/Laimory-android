package com.soma369.laimory.core.domain.usecase.notice

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.domain.repository.NoticeRepository
import com.soma369.laimory.core.domain.repository.PopupNoticeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PopupNoticeUseCasesTest {
    private val popupRepository = FakePopupNoticeRepository()
    private val noticeRepository = FakeNoticeRepository()

    @Test
    fun `이미 본 것을 빼고 서버 순서대로 돌려준다`() =
        runTest {
            popupRepository.ids = listOf(12, 9, 5)
            popupRepository.seen += 9

            val notices = GetPopupNoticesUseCase(popupRepository)()

            assertEquals(listOf(12L, 5L), notices.map(Notice::id))
        }

    @Test
    fun `한 건을 못 받으면 그 건만 건너뛴다`() =
        runTest {
            // id 를 받은 직후 관리자가 숨기면 단건 조회가 404 다.
            popupRepository.ids = listOf(12, 9)
            popupRepository.missing += 12

            assertEquals(listOf(9L), GetPopupNoticesUseCase(popupRepository)().map(Notice::id))
        }

    @Test
    fun `팝업 id 를 못 받으면 아무것도 띄우지 않는다`() =
        runTest {
            popupRepository.idsFailure = ApiException.NetworkException()

            assertTrue(GetPopupNoticesUseCase(popupRepository)().isEmpty())
        }

    @Test
    fun `본 기록을 못 읽으면 아무것도 띄우지 않는다`() =
        runTest {
            // 띄우면 닫아도 기록이 안 남아 콜드 스타트마다 다시 뜨는 팝업이 될 수 있다.
            popupRepository.ids = listOf(12)
            popupRepository.seenFailure = IllegalStateException("저장소")

            assertTrue(GetPopupNoticesUseCase(popupRepository)().isEmpty())
        }

    @Test
    fun `닫기만 했으면 본 것으로만 남기고 공지 읽음은 남기지 않는다`() =
        runTest {
            MarkPopupNoticeSeenUseCase(popupRepository, noticeRepository)(notice(12), opened = false)

            assertEquals(setOf(12L), popupRepository.seen)
            assertTrue(noticeRepository.read.isEmpty())
        }

    @Test
    fun `원문을 열었으면 공지 읽음에도 남기고 기존 읽음은 지우지 않는다`() =
        runTest {
            noticeRepository.read += 3

            MarkPopupNoticeSeenUseCase(popupRepository, noticeRepository)(notice(12), opened = true)

            assertEquals(setOf(12L), popupRepository.seen)
            assertEquals(setOf(3L, 12L), noticeRepository.read)
        }

    private class FakePopupNoticeRepository : PopupNoticeRepository {
        var ids: List<Long> = emptyList()
        var idsFailure: Throwable? = null
        var seenFailure: Throwable? = null
        val seen = mutableSetOf<Long>()
        val missing = mutableSetOf<Long>()

        override suspend fun getPopupNoticeIds(): List<Long> = idsFailure?.let { throw it } ?: ids

        override suspend fun getNotice(noticeId: Long): Notice {
            if (noticeId in missing) throw ApiException.ClientException(rawCode = 404, errorCode = -404)
            return notice(noticeId)
        }

        override suspend fun getSeenIds(): Set<Long> = seenFailure?.let { throw it } ?: seen.toSet()

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

    private companion object {
        fun notice(id: Long) = Notice(id, "공지 $id", "https://www.laimory.app/notices/$id", LocalDateTime.of(2026, 10, 8, 10, 0))
    }
}
