package com.soma369.laimory.core.data.model.inquiry.response

import com.soma369.laimory.core.domain.model.inquiry.InquiryDetail
import com.soma369.laimory.core.domain.model.inquiry.InquiryStatus
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/** 내 문의 한 건. 첨부는 만료 없는 CDN 주소로, 보낸 순서대로 온다. */
@Serializable
data class InquiryDetailResponse(
    val inquiryId: Long,
    val title: String,
    val status: String,
    val email: String,
    val description: String,
    val attachmentUrls: List<String> = emptyList(),
    val createdAt: String,
    val answeredAt: String? = null,
)

/** 접수 시각을 읽지 못하면 화면에 쓸 수 없어 `null` 이다(호출부가 실패로 다룬다). */
internal fun InquiryDetailResponse.toDomain(): InquiryDetail? {
    val created = runCatching { LocalDateTime.parse(createdAt) }.getOrNull() ?: return null
    return InquiryDetail(
        id = inquiryId,
        title = title,
        status = InquiryStatus.fromName(status),
        email = email,
        description = description,
        attachmentUrls = attachmentUrls,
        createdAt = created,
        answeredAt = answeredAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
    )
}
