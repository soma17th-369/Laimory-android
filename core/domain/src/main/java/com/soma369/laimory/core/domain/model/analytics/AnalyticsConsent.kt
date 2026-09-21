package com.soma369.laimory.core.domain.model.analytics

/**
 * 제품 분석 수집 동의.
 *
 * 미결정과 거부를 한 값으로 두지 않는다 — 합치면 "아직 묻지 않음"과 "싫다고 함"이 구분되지 않아
 * 동의 화면을 다시 띄울지 정할 수 없다.
 */
enum class AnalyticsConsent {
    /** 아직 묻지 않았다. 수집 여부는 빌드 기본값을 따른다. */
    UNDECIDED,
    GRANTED,
    DENIED,
}
