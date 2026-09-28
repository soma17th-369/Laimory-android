package com.soma369.laimory.core.domain.model.notice

import java.time.LocalDateTime

/**
 * 공지 한 건.
 *
 * 원문은 담지 않는다 — [contentUrl] 이 가리키는 게시된 페이지가 원문을 갖는다(약관과 같은 구조).
 *
 * @param publishedAt 게시 시각. 서버가 offset 없는 Asia/Seoul 벽시계로 준다.
 */
data class Notice(
    val id: Long,
    val title: String,
    val contentUrl: String,
    val publishedAt: LocalDateTime,
)
