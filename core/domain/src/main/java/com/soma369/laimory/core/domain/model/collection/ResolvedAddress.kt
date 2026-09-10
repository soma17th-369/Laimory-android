package com.soma369.laimory.core.domain.model.collection

/**
 * 좌표를 해석한 주소.
 *
 * [line] 은 한 줄 전체 주소로 목록·말풍선이 쓰고, [city]·[district] 는 홈 위치 카드가 두 층위로
 * 나눠 보여 주는 값이다(`서울시 · 역삼동`). 층위를 나누는 규칙은 플랫폼 주소 필드를 읽는
 * 자리가 안다 — 지역마다 어느 필드가 차는지 다르므로 도메인이 그 매핑을 알 이유가 없다.
 *
 * [city]·[district] 는 비어 있을 수 있다. 그때는 [line] 을 그대로 쓴다.
 */
data class ResolvedAddress(
    val line: String,
    val city: String? = null,
    val district: String? = null,
)
