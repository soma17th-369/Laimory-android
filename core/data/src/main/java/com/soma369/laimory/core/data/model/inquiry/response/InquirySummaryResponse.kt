package com.soma369.laimory.core.data.model.inquiry.response

import com.soma369.laimory.core.domain.model.inquiry.InquiryStatus
import com.soma369.laimory.core.domain.model.inquiry.InquirySummary
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/** 내 문의 목록의 한 건. 시각은 offset 없는 한국 벽시계다. */
@Serializable
data class InquirySummaryResponse(
    val inquiryId: Long,
    val title: String,
    val status: String,
    val createdAt: String,
    val answeredAt: String? = null,
)

/** 접수 시각을 읽지 못한 한 건 때문에 목록 전체를 잃지 않도록 그 건만 버린다. */
internal fun InquirySummaryResponse.toDomain(): InquirySummary? {
    val created = runCatching { LocalDateTime.parse(createdAt) }.getOrNull() ?: return null
    return InquirySummary(
        id = inquiryId,
        title = title,
        status = InquiryStatus.fromName(status),
        createdAt = created,
        answeredAt = answeredAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
    )
}
