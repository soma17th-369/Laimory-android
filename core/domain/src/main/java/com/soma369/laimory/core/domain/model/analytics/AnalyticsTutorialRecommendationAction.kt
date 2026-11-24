package com.soma369.laimory.core.domain.model.analytics

/** 홈 추천 날짜 장에서 고른 행동. */
enum class AnalyticsTutorialRecommendationAction {
    /** 추천한 날을 골랐다. */
    PICK_RECOMMENDED,

    /** `다른 날 직접 고르기` 로 날짜 피커를 열었다. */
    PICK_OTHER_DATE,

    /** `사진 권한 허용하기` · `사진 더 허용하기`. 여러 번 누를 수 있다. */
    REQUEST_PHOTO_ACCESS,

    /** `건너뛰기` 나 뒤로가기로 튜토리얼을 끝냈다. */
    SKIP,
}
