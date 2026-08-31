package com.soma369.laimory.core.domain.model.terms

import java.time.LocalDateTime

/**
 * 이 계정이 언제 어떤 버전에 동의했는지의 기록.
 *
 * 문서 버전이 불변이라 [document] 만으로 동의 당시 원문이 그대로 재현된다.
 * [acceptedAt] 은 서버가 기록한 값이며 앱이 만들지 않는다.
 */
data class TermAgreement(
    val document: TermDocument,
    val acceptedAt: LocalDateTime,
)
