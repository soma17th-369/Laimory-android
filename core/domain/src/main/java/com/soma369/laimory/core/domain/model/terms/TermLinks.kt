package com.soma369.laimory.core.domain.model.terms

/**
 * 로그인 화면과 설정이 여는 약관 두 건.
 *
 * 둘 다 없을 수 있다 — catalog 가 아직 활성화되지 않았거나 조회가 실패한 경우다. 그때 화면은
 * 링크만 비활성으로 두고 나머지는 그대로 보인다.
 */
data class TermLinks(
    val termsOfService: TermDocument? = null,
    val privacyPolicy: TermDocument? = null,
)
