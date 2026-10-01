package com.soma369.laimory.core.domain.model.notice

/**
 * 공지 목록과 그중 새 공지.
 *
 * @param notices 서버 정렬(최신 순) 그대로다.
 * @param newIds [NewNoticePolicy] 가 새 공지로 본 id.
 */
data class NoticeFeed(
    val notices: List<Notice>,
    val newIds: Set<Long>,
) {
    val hasNew: Boolean get() = newIds.isNotEmpty()
}
