package com.soma369.laimory.core.domain.model.terms

/**
 * 약관 동의를 요구하는 단계. 서버 enforcement 와 같은 축이다.
 *
 * [requiredTypes] 는 서버가 그 단계에서 실제로 요구하는 집합과 같아야 한다. 서버는 요청한 종류에
 * 현재 유효 문서가 하나라도 없으면 그 단계를 통째로 열어 주므로(fail-open), 앱도 조회되지 않은
 * 종류를 요구로 세우지 않는다.
 */
enum class TermStage(val requiredTypes: List<TermType>) {
    /** 로그인 직후. 미동의면 인증 API 대부분이 막힌다. */
    LOGIN(listOf(TermType.TERMS_OF_SERVICE)),

    /**
     * 타임라인 초안 생성과 사진 업로드 발급.
     *
     * 실제로 켠 데이터 소스와 무관하게 세 종류를 모두 요구한다 — 서버 판정이 그렇다. 헬스를 켜지
     * 않은 사용자에게도 민감정보 동의를 받아야 한다.
     */
    TIMELINE_FIRST_CREATE(
        listOf(
            TermType.SENSITIVE_INFORMATION_CONSENT,
            TermType.THIRD_PARTY_PROVISION_CONSENT,
            TermType.CROSS_BORDER_TRANSFER_CONSENT,
        ),
    ),
}
