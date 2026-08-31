package com.soma369.laimory.core.domain.model.terms

/**
 * 앱이 다루는 약관 종류.
 *
 * 여기에 없는 종류가 응답에 섞여 오면 매핑에서 버린다 — 모르는 약관을 화면에 올려 봐야
 * 사용자가 판단할 근거가 없다.
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

    /**
     * 위치기반서비스 이용약관.
     *
     * 단계 일괄 동의가 아니라 **위치정보가 실제로 실려 나가는 요청에만** 필요하다. 위치를 빼고
     * 만드는 타임라인은 이 동의 없이도 만들 수 있다.
     */
    LOCATION_BASED_SERVICE_TERMS,
}
