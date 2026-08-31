package com.soma369.laimory.core.data.model.terms.request

import com.soma369.laimory.core.domain.model.terms.TermDocument
import kotlinx.serialization.Serializable

/**
 * 동의 일괄 등록 요청.
 *
 * 수락 시각은 보내지 않는다 — 서버가 기록한다. 조회 응답의 `(종류, 버전)` 을 그대로 회신하는
 * 계약이라 버전 문자열을 앱에서 만들거나 가공하지 않는다.
 */
@Serializable
data class TermAgreementCreateRequest(
    val agreements: List<TermAgreementItem>,
)

@Serializable
data class TermAgreementItem(
    val termType: String,
    val version: String,
)

internal fun List<TermDocument>.toRequest() =
    TermAgreementCreateRequest(
        agreements = map { TermAgreementItem(termType = it.termType.name, version = it.version) },
    )
