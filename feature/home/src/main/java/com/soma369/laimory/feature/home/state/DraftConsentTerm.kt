package com.soma369.laimory.feature.home.state

/** 초안 생성 전 필수 동의 항목. 모든 항목이 독립적으로 선택돼야 생성 CTA 가 활성화된다. */
enum class DraftConsentTerm {
    /** 민감정보(건강정보) 처리 동의. */
    SENSITIVE_INFO,

    /** 개인정보 제3자 제공 동의. */
    THIRD_PARTY_PROVISION,

    /** 개인정보 국외 이전 고지·동의. */
    OVERSEAS_TRANSFER,
}
