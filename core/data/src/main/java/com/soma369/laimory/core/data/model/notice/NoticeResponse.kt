package com.soma369.laimory.core.data.model.notice

import com.soma369.laimory.core.domain.model.notice.Notice
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/** 공지 한 건. 원문은 담기지 않고 [contentUrl] 페이지가 갖는다. */
@Serializable
data class NoticeResponse(
    val noticeId: Long,
    val title: String,
    val contentUrl: String,
    val publishedAt: String,
)

/** 게시 시각을 읽지 못한 한 건 때문에 목록 전체를 잃지 않도록 그 건만 버린다. */
internal fun NoticeResponse.toDomain(): Notice? {
    val published = runCatching { LocalDateTime.parse(publishedAt) }.getOrNull() ?: return null
    return Notice(
        id = noticeId,
        title = title,
        contentUrl = contentUrl,
        publishedAt = published,
    )
}
