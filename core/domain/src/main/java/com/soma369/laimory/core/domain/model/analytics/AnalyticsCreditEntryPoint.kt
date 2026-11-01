package com.soma369.laimory.core.domain.model.analytics

/** 크레딧 화면으로 들어온 자리. */
enum class AnalyticsCreditEntryPoint {
    /** 설정의 크레딧 항목. */
    SETTINGS,

    /** 타임라인 만들기 확인 다이얼로그의 `충전하러 가기`(크레딧 부족). */
    CREATE_CONFIRM_CHARGE,

    /** 같은 다이얼로그의 크레딧 줄. */
    CREATE_CONFIRM_BALANCE,

    /** 생성 요청이 크레딧 부족으로 실패한 로딩 화면의 `충전하러 가기`. */
    DRAFT_LOADING_CHARGE,
}
