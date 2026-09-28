package com.soma369.laimory.core.domain.model.notice

import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import javax.inject.Inject

/**
 * 새 공지 표시 규칙. **아직 눌러 보지 않았고, 게시된 지 [NEW_PERIOD] 가 지나지 않은** 공지가 새 공지다.
 *
 * 기간을 두는 이유: 공지는 점검 안내처럼 때가 지나면 소식이 아니다. 기간이 없으면 안 읽는 사람과
 * 새로 설치한 사람에게 지난 공지까지 계속 떠 늘 켜져 있는 점이 되고, 그러면 정말 새 공지가 와도
 * 알아보지 못한다. 꼭 읽혀야 하는 공지는 표시가 아니라 다른 경로(푸시·안내창)가 맡을 일이다.
 */
class NewNoticePolicy
    @Inject
    constructor(
        private val clock: Clock,
    ) {
        fun newNoticeIds(
            notices: List<Notice>,
            readIds: Set<Long>,
        ): Set<Long> {
            val since = clock.instant().minus(NEW_PERIOD)
            return notices
                .filter { notice -> notice.id !in readIds && !notice.publishedInstant().isBefore(since) }
                .mapTo(mutableSetOf(), Notice::id)
        }

        /** 읽음 기록을 남겨 둘 공지. 기간이 지나면 새 공지가 될 수 없어 기록도 필요 없다. */
        fun trackedIds(notices: List<Notice>): Set<Long> {
            val since = clock.instant().minus(NEW_PERIOD)
            return notices.filterNot { it.publishedInstant().isBefore(since) }.mapTo(mutableSetOf(), Notice::id)
        }

        private fun Notice.publishedInstant() = publishedAt.atZone(PUBLISHED_ZONE).toInstant()

        companion object {
            /** 처리방침 개정 공지 기간(시행 7일 전)과 같다. */
            val NEW_PERIOD: Duration = Duration.ofDays(7)

            /** 서버가 게시 시각을 offset 없는 한국 벽시계로 준다. */
            private val PUBLISHED_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        }
    }
