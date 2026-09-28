package com.soma369.laimory.core.domain.model.notice

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class NewNoticePolicyTest {
    // 한국 시각 2026-09-28 12:00.
    private val policy = NewNoticePolicy(Clock.fixed(Instant.parse("2026-09-28T03:00:00Z"), ZoneOffset.UTC))

    @Test
    fun `읽지 않았고 7일이 지나지 않은 공지만 새 공지다`() {
        val notices =
            listOf(
                notice(id = 3, publishedAt = LocalDateTime.of(2026, 9, 27, 9, 0)),
                notice(id = 2, publishedAt = LocalDateTime.of(2026, 9, 25, 9, 0)),
                notice(id = 1, publishedAt = LocalDateTime.of(2026, 9, 1, 9, 0)),
            )

        assertEquals(setOf(3L), policy.newNoticeIds(notices, readIds = setOf(2L)))
    }

    @Test
    fun `안 읽어도 7일이 지나면 새 공지가 아니다`() {
        // 표시가 계속 켜져 있으면 늘 떠 있는 점이 되어, 정말 새 공지가 와도 알아보지 못한다.
        val notices =
            listOf(
                notice(id = 2, publishedAt = LocalDateTime.of(2026, 9, 21, 12, 0)),
                notice(id = 1, publishedAt = LocalDateTime.of(2026, 9, 21, 11, 59)),
            )

        assertEquals(setOf(2L), policy.newNoticeIds(notices, readIds = emptySet()))
    }

    @Test
    fun `게시 시각은 한국 벽시계로 읽는다`() {
        // 서버가 offset 없이 KST 로 준다. UTC 로 읽으면 9시간 늦은 글이 되어 아직 새 공지로 남는다.
        val notice = notice(id = 1, publishedAt = LocalDateTime.of(2026, 9, 21, 11, 0))

        assertEquals(emptySet<Long>(), policy.newNoticeIds(listOf(notice), readIds = emptySet()))
    }

    @Test
    fun `읽음 기록은 기간 안의 공지만 남긴다`() {
        val notices =
            listOf(
                notice(id = 2, publishedAt = LocalDateTime.of(2026, 9, 27, 9, 0)),
                notice(id = 1, publishedAt = LocalDateTime.of(2026, 9, 1, 9, 0)),
            )

        assertEquals(setOf(2L), policy.trackedIds(notices))
    }

    private fun notice(
        id: Long,
        publishedAt: LocalDateTime,
    ) = Notice(
        id = id,
        title = "공지 $id",
        contentUrl = "https://www.laimory.app/notices/$id",
        publishedAt = publishedAt,
    )
}
