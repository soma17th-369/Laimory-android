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

    /**
     * 위치정보를 실어 보내는 초안 생성.
     *
     * 다른 단계와 달리 **요청 내용에 따라 필요해진다.** 서버도 위치정보가 실제로 처리되는
     * 요청에만 강제하고, 미동의면 위치를 뺀 타임라인은 그대로 허용한다. 그래서 이것을
     * [TIMELINE_FIRST_CREATE] 에 합치지 않는다 — 합치면 위치를 한 번도 보내지 않을 사용자에게도
     * 동의를 받게 된다.
     */
    TIMELINE_LOCATION(listOf(TermType.LOCATION_BASED_SERVICE_TERMS)),
}
