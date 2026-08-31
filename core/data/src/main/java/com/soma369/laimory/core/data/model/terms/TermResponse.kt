package com.soma369.laimory.core.data.model.terms

import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * 약관 문서 한 건.
 *
 * `termType` 을 enum 이 아니라 문자열로 받는다 — 서버 catalog 에는 앱이 아직 다루지 않는 종류가
 * 있고 앞으로 더 늘 수 있다. enum 으로 받으면 모르는 값 하나에 응답 전체의 역직렬화가 깨진다.
 */
@Serializable
data class TermResponse(
    val termType: String,
    val version: String,
    val title: String,
    val contentUrl: String,
    val effectiveAt: String,
)

/** 앱이 다루지 않는 종류나 해석할 수 없는 시각은 `null` 로 떨어져 호출부가 버린다. */
internal fun TermResponse.toDomain(): TermDocument? {
    val type = TermType.entries.firstOrNull { it.name == termType } ?: return null
    val effective = runCatching { LocalDateTime.parse(effectiveAt) }.getOrNull() ?: return null
    return TermDocument(
        termType = type,
        version = version,
        title = title,
        contentUrl = contentUrl,
        effectiveAt = effective,
    )
}
