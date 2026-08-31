package com.soma369.laimory.core.domain.model.terms

import java.time.LocalDateTime

/**
 * 현재 유효한 약관 문서 한 건.
 *
 * 원문은 앱이 들고 있지 않다. [contentUrl] 이 가리키는 게시된 page 가 원문의 정본이며, 그 주소는
 * 버전마다 불변이라 지난 동의도 같은 주소로 재현된다.
 *
 * [version] 은 동의를 등록할 때 **조회 응답 값 그대로** 돌려보내야 하는 식별자다. 형식을 해석하거나
 * 비교 규칙을 만들지 않는다 — 서버가 현재 유효 버전인지 판정한다.
 */
data class TermDocument(
    val termType: TermType,
    val version: String,
    val title: String,
    val contentUrl: String,
    /** 효력 시작 시각(Asia/Seoul 벽시계). 시간대 정보가 없는 값이다. */
    val effectiveAt: LocalDateTime,
)
