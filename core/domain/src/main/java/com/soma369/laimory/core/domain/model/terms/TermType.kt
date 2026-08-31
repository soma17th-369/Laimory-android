package com.soma369.laimory.core.domain.model.terms

/**
 * 앱이 다루는 약관 종류.
 *
 * 서버 catalog 에는 위치기반서비스 이용약관까지 있지만, 그 조건부 동의는 서버가 아직 어느
 * endpoint 에도 gate 를 붙이지 않아 요청하지 않는다. 여기에 없는 종류가 응답에 섞여 오면
 * 매핑에서 버린다 — 모르는 약관을 화면에 올려 봐야 사용자가 판단할 근거가 없다.
 */
enum class TermType {
    /** 이용약관. 로그인 단계 필수 동의 대상이다. */
    TERMS_OF_SERVICE,

    /** 개인정보 처리방침. **동의 대상이 아니라 상시 공개 문서**다. */
    PRIVACY_POLICY,

    /** 민감정보(건강정보) 처리 동의. */
    SENSITIVE_INFORMATION_CONSENT,

    /** 개인정보 제3자 제공 동의. */
    THIRD_PARTY_PROVISION_CONSENT,

    /** 개인정보 국외 이전 동의. */
    CROSS_BORDER_TRANSFER_CONSENT,
}
