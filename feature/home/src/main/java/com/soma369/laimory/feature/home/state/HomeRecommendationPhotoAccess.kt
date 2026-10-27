package com.soma369.laimory.feature.home.state

/**
 * 추천을 계산할 때의 사진 접근. 추천이 비었을 때 왜 비었는지와 무엇을 권할지를 가른다.
 *
 * 권한이 없으면 사진을 하나도 읽지 못해 추천이 비고, 일부만 허용했으면 고른 사진 안에서만 센다. 둘 다 사진이 없는 것이
 * 아니라 못 읽은 것이라 `찾지 못했어요` 대신 허용을 권한다.
 */
enum class HomeRecommendationPhotoAccess {
    /** 전체 사진을 읽는다. */
    FULL,

    /** Android 14+ 에서 사용자가 고른 일부 사진만 읽는다. */
    LIMITED,

    /** 사진을 읽지 못한다. */
    NONE,
}
