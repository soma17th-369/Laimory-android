package com.soma369.laimory.core.data.model.terms

import com.soma369.laimory.core.domain.model.terms.TermAgreement
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/** 동의 이력 한 건. 문서 정보는 동의한 그 버전의 것이다. */
@Serializable
data class TermAgreementResponse(
    val termType: String,
    val version: String,
    val title: String,
    val contentUrl: String,
    val effectiveAt: String,
    val acceptedAt: String,
)

internal fun TermAgreementResponse.toDomain(): TermAgreement? {
    val document =
        TermResponse(
            termType = termType,
            version = version,
            title = title,
            contentUrl = contentUrl,
            effectiveAt = effectiveAt,
        ).toDomain() ?: return null
    val accepted = runCatching { LocalDateTime.parse(acceptedAt) }.getOrNull() ?: return null
    return TermAgreement(document = document, acceptedAt = accepted)
}
